package riscv_cpu

import chisel3._
import chisel3.util._

import config._
import utility.ReplacementPolicy

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import freechips.rocketchip.util.Annotated.srams
import riscv_cpu.LS_state.s_wait_valid
import riscv_cpu.bus_state.s_wait_ready
import riscv_cpu.bus_state.s_busy
import scala.collection.Parallel

class IFU_catch extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val valid = Input(Bool())
        val inst  = Input(UInt(32.W))
    })
    setInline("IFU_catch.v",
    """module IFU_catch(
    |    input clock,
    |    input valid,
    |    input [31:0] inst
    |);
    |
    |   import "DPI-C" function void IFU_catch(input int unsigned inst);
    |   always @(posedge clock) begin
    |       if(valid) begin
    |           IFU_catch(inst);
    |       end
    |   end
    |
    |endmodule
    """.stripMargin)
}

class Icache_catch extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle{
        val Icache = Input(Bool())
        val map_hit = Input(Bool())
        val cache_hit = Input(Bool())
    })
    setInline("Icache_catch.v",
    """module Icache_catch(
    |   input Icache,
    |   input map_hit,
    |   input cache_hit
    |);
    |
    |   import "DPI-C" function void Icache_catch(input int unsigned map_hit, input int unsigned cache_hit);
    |   always @(posedge Icache) begin
    |       Icache_catch({31'b0, map_hit}, {31'b0, cache_hit});
    |   end
    |
    |endmodule
    """.stripMargin)
}

class Icache_state_catch extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle {
        val valid = Input(Bool())

        val write_index = Input(UInt(32.W))
        val write_way = Input(UInt(32.W))
        val write_tag = Input(UInt(32.W))
        val write_data = Input(UInt((Config.Icache_Param.block_size * 8).W))
    })

    val code = 
    s"""
    |module Icache_state_catch(
    |    input valid,
    |
    |    input [31:0] write_index,
    |    input [31:0] write_way,
    |    input [31:0] write_tag,
    |    input [${Config.Icache_Param.block_size * 8 - 1}:0] write_data
    |);
    |
    |   import "DPI-C" function void Icache_state_catch(input int unsigned write_index, input int unsigned write_way, input int unsigned write_tag, input bit [${Config.Icache_Param.block_size * 8 - 1}:0] write_data);
    |   always @(posedge valid) begin
    |       Icache_state_catch(write_index, write_way, write_tag, write_data);
    |   end
    |
    |endmodule
    """

    setInline("Icache_state_catch.v", code.stripMargin)
}

class Icache_MAT_catch extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle {
        val valid = Input(Bool())
        val count = Input(UInt(32.W))
    })

    setInline("Icache_MAT_catch.v",
    """module Icache_MAT_catch(
    |    input valid,
    |    input [31:0] count
    |);
    |
    |   import "DPI-C" function void Icache_MAT_catch(input int unsigned count);
    |   always @(posedge valid) begin
    |       Icache_MAT_catch(count);
    |   end
    |
    |endmodule
    """.stripMargin)

}

class Icache(address: Seq[AddressSet], way: Int, set: Int, block_size: Int) extends Module {
    val io = IO(new Bundle{
        val addr = Flipped(ValidIO(Input(UInt(32.W))))
        val data = Output(UInt((Config.Icache_Param.block_size * 8).W))

        val cache_hit = Output(Bool())
        val replace_data = Flipped(ValidIO(Input(UInt((Config.Icache_Param.block_size * 8).W))))
        val replace_addr = Input(UInt(32.W))
    })

    val valid_width = log2Ceil(address.map(_.mask).reduce(_ max _))
    val offset_width = log2Ceil(block_size)
    val set_width = log2Ceil(set)
    val tag_width = valid_width - offset_width - set_width

    // a vector(Way) of Mem(Set)
    val cache_valids = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(false.B)))))
    val cache_tags = Mem(set, Vec(way, UInt((tag_width).W)))
    val cache_data = Mem(set, Vec(way, UInt((block_size * 8).W)))

    val set_index = io.addr.bits(set_width + offset_width - 1, offset_width)
    val tag = io.addr.bits(valid_width - 1, set_width + offset_width)

    val cache_way_valids = cache_valids(set_index)
    val cache_way_tags = cache_tags.read(set_index)
    val cache_way_datas = cache_data.read(set_index)

    // class Cache_Meta extends Bundle{
    //     val valid = Bool()
    //     val tag = UInt(tag_width.W)
    // }

    // val metas = meta.read(set_index).map{c => 
    //     c.asTypeOf(new Cache_Meta)
    // }
    
    val tag_equal_vec = VecInit(cache_way_tags.map(_ === tag))
    val tag_match_vec = tag_equal_vec.zip(cache_way_valids).map{case (a, b) => a && b}
    val tag_match = tag_match_vec.reduce(_ | _)
    val match_way = Mux1H(tag_match_vec, (0 until way).map(_.U))
    
    io.data := cache_way_datas(match_way)
    io.cache_hit := tag_match

    val replace_set_index = io.replace_addr(set_width + offset_width - 1, offset_width) // input addr maybe change after input shake hands
    
    val replacement = ReplacementPolicy.fromString("setlru", way, set)
    
    val replace_way = replacement.way(replace_set_index)
    val replace_way_mask = UIntToOH(replace_way, way)

    val replace_tag = io.replace_addr(valid_width - 1, set_width + offset_width)
    val replace_tag_v = VecInit((0 until way).map(_ => replace_tag))

    val replace_cache = io.replace_data.bits
    val replace_cache_v = VecInit((0 until way).map(_ => replace_cache))

    when(io.replace_data.valid){
        cache_valids(replace_set_index)(replace_way) := true.B
        cache_tags.write(replace_set_index, replace_tag_v, replace_way_mask.asBools)
        cache_data.write(replace_set_index, replace_cache_v, replace_way_mask.asBools)
        replacement.access(replace_set_index, replace_way)
    }.elsewhen(RegNext(tag_match && io.addr.valid)){
        replacement.access(replace_set_index, match_way)
    }

    if(Config.Simulate){
        val Icache_state = Module(new Icache_state_catch)
        Icache_state.io.valid := io.replace_data.valid
        Icache_state.io.write_index := replace_set_index
        Icache_state.io.write_way := replace_way
        Icache_state.io.write_tag := replace_tag
        Icache_state.io.write_data := replace_cache
    }
}

object IFU_state extends ChiselEnum{
  val s_wait_valid,
      s_wait_ready,
      s_send_addr,
      s_get,
      s_replacement_send_addr,
      s_replacement_get
      = Value
}

class IFU(idBits: Int)(implicit p: Parameters) extends LazyModule {
    val masterNode = AXI4MasterNode(p(ExtIn).map(params =>
        AXI4MasterPortParameters(
        masters = Seq(AXI4MasterParameters(
            name = "ifu",
            id   = IdRange(0, 1 << idBits))))).toSeq)
    lazy val module = new Impl
    class Impl extends LazyModuleImp(this) {
        val io = IO(new Bundle{
            val WBU_2_IFU = Flipped(Decoupled(Input(new BUS_WBU_2_IFU)))
            val REG_2_IFU = Input(new BUS_REG_2_IFU)
            val IFU_2_IDU = Decoupled(Output(new BUS_IFU_2_IDU))
            val IFU_2_REG = Output(new BUS_IFU_2_REG)
        })
        // val state = RegInit(IFU_state.s_wait_valid)
        
        // val (master, _) = masterNode.out(0)

        // val Icache = Module(new Icache(Config.Icache_Param.address, Config.Icache_Param.way, Config.Icache_Param.set, Config.Icache_Param.block_size))
        // Icache.io.addr.bits := io.REG_2_IFU.Next_PC
        // Icache.io.addr.valid := io.WBU_2_IFU.fire

        // val addr_cache = RegEnable(io.REG_2_IFU.Next_PC, io.WBU_2_IFU.fire) // cache addr is very useful
        // val inst_cache = RegEnable(Icache.io.data, io.WBU_2_IFU.fire)

        // io.IFU_2_IDU.valid := state === IFU_state.s_wait_ready
        // io.IFU_2_IDU.bits.PC := addr_cache
        // io.IFU_2_IDU.bits.data := inst_cache

        // val burst_counter = RegInit(0.U(32.W))
        // when(io.WBU_2_IFU.fire){
        //     burst_counter := burst_counter + 1.U
        // }

        val state = RegInit(IFU_state.s_wait_valid)
        
        val (master, _) = masterNode.out(0)

        val Icache = Module(new Icache(Config.Icache_Param.address, Config.Icache_Param.way, Config.Icache_Param.set, Config.Icache_Param.block_size))
        Icache.io.addr.bits := io.REG_2_IFU.Next_PC
        Icache.io.addr.valid := io.WBU_2_IFU.fire

        val block_num = Config.Icache_Param.block_size / 4

        val addr_cache = RegEnable(io.REG_2_IFU.Next_PC, io.WBU_2_IFU.fire) // cache addr is very useful
        val block_index_pre = io.REG_2_IFU.Next_PC(log2Ceil(Config.Icache_Param.block_size), 2)
        val blcok_index = addr_cache(log2Ceil(Config.Icache_Param.block_size), 2)

        val Multi_transfer = RegInit(VecInit(Seq.fill(block_num)(0.U(32.W))))
        // val Multi_transfer_counter = RegInit((block_num - 1).U)
        val Multi_transfer_counter = RegInit(0.U(log2Ceil(block_num).W))
        when (master.r.fire && state === IFU_state.s_replacement_get) {
            when(Multi_transfer_counter === (block_num - 1).U){
                Multi_transfer_counter := 0.U
            }.otherwise{
                Multi_transfer_counter := Multi_transfer_counter + 1.U
            }

            for(i <- 0 until (block_num - 1)){
                Multi_transfer(i) := Multi_transfer(i + 1)
            }
            Multi_transfer(block_num - 1) := master.r.bits.data
        }.elsewhen(io.WBU_2_IFU.fire){
            Multi_transfer(block_index_pre) := Icache.io.data.asTypeOf(Vec(Config.Icache_Param.block_size / 4, UInt(32.W)))(block_index_pre)
        }
        io.WBU_2_IFU.ready := state === IFU_state.s_wait_valid

        io.IFU_2_IDU.valid := Mux(state === IFU_state.s_get, master.r.valid, state === IFU_state.s_wait_ready)
        io.IFU_2_IDU.bits.PC := addr_cache

        master.ar.valid := (state === IFU_state.s_replacement_send_addr) || (state === IFU_state.s_send_addr)
        master.ar.bits.len := Mux(state === IFU_state.s_replacement_send_addr, (block_num - 1).U, 0.U)
        master.ar.bits.addr := Mux(state === IFU_state.s_send_addr, addr_cache, 
            (addr_cache & ~((Config.Icache_Param.block_size - 1).U(32.W))) + (Multi_transfer_counter << 2.U)) // so we can use it here
        Icache.io.replace_addr := addr_cache
        
        val map_hit = Config.Icache_Param.address.map(_.contains(io.REG_2_IFU.Next_PC)).reduce(_ || _)
        master.r.ready := Mux(state === IFU_state.s_get, io.IFU_2_IDU.ready, state === IFU_state.s_replacement_get)

        Icache.io.replace_data.bits := Multi_transfer.asTypeOf(UInt((Config.Icache_Param.block_size * 8).W))
        Icache.io.replace_data.valid := (state === IFU_state.s_wait_ready) && (RegNext(state === IFU_state.s_replacement_get))

        val inst_cache = Mux(state === IFU_state.s_get, master.r.bits.data, Multi_transfer(blcok_index))
            
        io.IFU_2_IDU.bits.data := inst_cache
        io.IFU_2_REG.GPR_Aaddr := inst_cache(19, 15)
        io.IFU_2_REG.GPR_Baddr := inst_cache(24, 20)

        state := MuxLookup(state, IFU_state.s_wait_valid)(
            Seq(
                IFU_state.s_wait_valid -> Mux(io.WBU_2_IFU.fire, 
                    MuxCase(IFU_state.s_wait_valid, 
                        Seq(
                            (Icache.io.cache_hit & map_hit)     -> IFU_state.s_wait_ready,
                            (!Icache.io.cache_hit & map_hit)    -> IFU_state.s_replacement_send_addr,
                            (!map_hit)                          -> IFU_state.s_send_addr,
                        )
                    ),
                    IFU_state.s_wait_valid
                ),

                IFU_state.s_wait_ready -> Mux(io.IFU_2_IDU.fire, 
                    IFU_state.s_wait_valid, 
                    IFU_state.s_wait_ready
                ),

                IFU_state.s_send_addr -> Mux(master.ar.fire, 
                    IFU_state.s_get, 
                    IFU_state.s_send_addr
                ),

                IFU_state.s_get -> Mux(master.r.fire, 
                    IFU_state.s_wait_valid,
                    IFU_state.s_get
                ),

                IFU_state.s_replacement_send_addr -> Mux(master.ar.fire, 
                    IFU_state.s_replacement_get, 
                    IFU_state.s_replacement_send_addr
                ),

                IFU_state.s_replacement_get -> Mux(master.r.fire && (Multi_transfer_counter === (block_num - 1).U), 
                    IFU_state.s_wait_ready, 
                    // Mux((Multi_transfer_counter === (block_num - 1).U), 
                    //     IFU_state.s_wait_ready, 
                    //     IFU_state.s_replacement_send_addr
                    // ),
                    IFU_state.s_replacement_get
                )
            )
        )

        if(Config.Simulate){
            val Catch = Module(new IFU_catch)
            Catch.io.clock := clock
            Catch.io.valid := io.IFU_2_IDU.fire && !reset.asBool
            Catch.io.inst := io.IFU_2_IDU.bits.data

            val cache_Catch = Module(new Icache_catch)
            cache_Catch.io.Icache := io.WBU_2_IFU.fire && !reset.asBool
            cache_Catch.io.map_hit := map_hit
            cache_Catch.io.cache_hit := Icache.io.cache_hit & map_hit

            val MAT_Counter = RegInit(0.U(32.W))
            when(io.WBU_2_IFU.fire){
                MAT_Counter := 1.U
            }.otherwise{
                MAT_Counter := MAT_Counter + 1.U
            }

            val MAT_Catch = Module(new Icache_MAT_catch)
            MAT_Catch.io.valid := io.IFU_2_IDU.fire && !reset.asBool
            MAT_Catch.io.count := MAT_Counter
        }

        // master ignore

        master.aw.valid := false.B
        master.aw.bits.addr := 0.U
        master.aw.bits.size := 0.U
        master.aw.bits.id    := 0.U
        master.aw.bits.len   := 0.U
        master.aw.bits.burst := 0.U
        master.aw.bits.lock  := 0.U
        master.aw.bits.cache := 0.U
        master.aw.bits.prot  := 0.U
        master.aw.bits.qos   := 0.U
        master.w.valid := false.B
        master.w.bits.data := 0.U
        master.w.bits.strb := 0.U
        master.w.bits.last  := 1.U
        master.b.ready := false.B

        master.ar.bits.size  := 2.U
        master.ar.bits.id    := 0.U
        master.ar.bits.burst := 1.U // INCR
        master.ar.bits.lock  := 0.U // Normal access
        master.ar.bits.cache := 0.U // Cacheable
        master.ar.bits.prot  := 0.U // Normal memory
        master.ar.bits.qos   := 0.U // Quality of Service
    }
}