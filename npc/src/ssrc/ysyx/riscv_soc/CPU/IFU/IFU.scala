package riscv_cpu

import chisel3._
import chisel3.util._

import config._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import freechips.rocketchip.util.Annotated.srams
import riscv_cpu.LS_state.s_wait_valid
import riscv_cpu.bus_state.s_wait_ready
import riscv_cpu.bus_state.s_busy

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

class Icache(address: Seq[AddressSet], way: Int, set: Int, block_size: Int) extends Module {
    val io = IO(new Bundle{
        val addr = Input(UInt(32.W))
        val data = Output(UInt(32.W))

        val cache_hit = Output(Bool())
        val replace_data = Flipped(ValidIO(Input(UInt(32.W))))
        val replace_addr = Input(UInt(32.W))
    })

    val valid_width = log2Ceil(address.map(_.mask).reduce(_ max _))
    val offset_width = log2Ceil(block_size)
    val set_width = log2Ceil(set)
    val tag_width = valid_width - offset_width - set_width

    val line_width = 1 + tag_width + block_size * 8
    val cache = Mem(set, UInt(line_width.W))

    val set_index = io.addr(set_width + offset_width - 1, offset_width)
    val tag = io.addr(valid_width - 1, set_width + offset_width)
    
    val cache_valid = cache(set_index)(line_width - 1)
    val cache_tag = cache(set_index)(line_width - 2, block_size * 8)
    io.data := cache(set_index)(block_size * 8 - 1, 0)
    
    io.cache_hit := cache_valid && (cache_tag === tag)

    // TODO: have to implement LRU Algorithm
    val replace_set_index = io.replace_addr(set_width + offset_width - 1, offset_width) // input addr maybe change after input shake hands
    val replace_tag = io.replace_addr(valid_width - 1, set_width + offset_width)
    val replace_cache = io.replace_data.bits

    when(io.replace_data.valid){
        cache(replace_set_index) := Cat(true.B, replace_tag, replace_cache)
    }

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
        val (master, _) = masterNode.out(0)

        val Icache = Module(new Icache(Config.Icache_Param.address, Config.Icache_Param.way, Config.Icache_Param.set, Config.Icache_Param.block_size))
        Icache.io.addr := io.REG_2_IFU.Next_PC

        val state = RegInit(bus_state.s_wait_valid)
        io.WBU_2_IFU.ready := state === bus_state.s_wait_valid

        val addr_cache = RegEnable(io.REG_2_IFU.Next_PC, io.WBU_2_IFU.fire) // cache addr is very useful

        io.IFU_2_IDU.valid := state === bus_state.s_wait_ready
        io.IFU_2_IDU.bits.PC := addr_cache

        master.ar.valid := state === s_busy
        master.ar.bits.addr := addr_cache // so we can use it here
        Icache.io.replace_addr := addr_cache
        
        val map_hit = Config.Icache_Param.address.map(_.contains(addr_cache)).reduce(_ || _)
        master.r.ready := state === bus_state.s_pipeline
        Icache.io.replace_data.bits := master.r.bits.data
        Icache.io.replace_data.valid := master.r.valid && map_hit // if not & map_hit, will cause an very subtle bug 

        val inst_cache = RegEnable(Mux(io.WBU_2_IFU.fire, 
            Icache.io.data, master.r.bits.data),
            io.WBU_2_IFU.fire || master.r.fire
        ) // cache inst

        io.IFU_2_IDU.bits.data := inst_cache
        io.IFU_2_REG.GPR_Aaddr := inst_cache(19, 15)
        io.IFU_2_REG.GPR_Baddr := inst_cache(24, 20)

        state := MuxLookup(state, bus_state.s_wait_valid)(
            Seq(
                bus_state.s_wait_valid -> Mux(io.WBU_2_IFU.fire, 
                    Mux(Icache.io.cache_hit && map_hit, 
                        bus_state.s_wait_ready, 
                        bus_state.s_busy
                    ), 
                    bus_state.s_wait_valid
                ),

                bus_state.s_wait_ready -> Mux(io.IFU_2_IDU.fire, 
                    bus_state.s_wait_valid, 
                    bus_state.s_wait_valid
                ),

                bus_state.s_busy -> Mux(master.ar.fire, 
                    bus_state.s_pipeline, // actually is not true `pipeline` state, but use for now
                    bus_state.s_busy
                ),

                bus_state.s_pipeline -> Mux(master.r.fire, 
                    bus_state.s_wait_ready, 
                    bus_state.s_pipeline
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
            cache_Catch.io.cache_hit := Icache.io.cache_hit
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
        master.ar.bits.len   := 0.U
        master.ar.bits.burst := 0.U
        master.ar.bits.lock  := 0.U
        master.ar.bits.cache := 0.U
        master.ar.bits.prot  := 0.U
        master.ar.bits.qos   := 0.U
    }
}