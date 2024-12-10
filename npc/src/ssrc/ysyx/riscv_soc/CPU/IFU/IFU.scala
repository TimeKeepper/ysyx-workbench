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

object Icache_state extends ChiselEnum{
    val idle, busy, get = Value
}

class Icache_output extends Bundle {
    val data = Output(UInt(32.W))
    val addr = Output(UInt(32.W))
}

class Icache(address: Seq[AddressSet], block_size : Int, block_num : Int) extends Module {
    val io = IO(new Bundle{
        val AXI = AXI4Bundle(CPUAXI4BundleParameters())
        val addr = Flipped(Decoupled(Input(UInt(32.W))))
        val data = Decoupled(new Icache_output)
    })
    val offset_width = log2Ceil(block_size)
    val index_width = log2Ceil(block_num)
    val tag_width = log2Ceil(address.map(_.mask).reduce(_ max _))
    
    val offset = io.addr.bits(offset_width - 1, 0)
    val index = io.addr.bits(index_width + offset_width - 1, offset_width)
    val tag = io.addr.bits(tag_width - 1, index_width + offset_width)

    val cache_size = 1 + tag_width - (index_width + offset_width) + (block_size * 8)
    val cache = Mem(block_num, UInt(cache_size.W))
    
    val cache_data = cache(index)(block_size * 8 - 1, 0)
    val cache_tag = cache(index)(cache_size - 1 - 1, block_size * 8)
    val cache_valid = cache(index)(cache_size - 1)

    val map_hit = address.map(_.contains(io.addr.bits)).reduce(_ || _)

    val cache_hit = cache_valid && cache_tag === tag && map_hit

    val state = RegInit(Icache_state.idle)

    state := MuxLookup(state, Icache_state.idle){
        Seq(
            Icache_state.idle -> Mux(io.addr.valid, Mux(cache_hit, Icache_state.get, Icache_state.busy), Icache_state.idle),
            Icache_state.busy -> Mux(io.AXI.r.fire, Icache_state.get, Icache_state.busy),
            Icache_state.get  -> Mux(io.data.ready, Icache_state.idle, Icache_state.get)
        )
    }

    io.addr.ready := state === Icache_state.idle
    io.data.valid := state === Icache_state.get
    io.AXI.ar.valid := state === Icache_state.busy
    io.AXI.r.ready := state === Icache_state.busy

    io.data.bits.addr := RegEnable(io.addr.bits, io.addr.fire)
    val data = RegInit(0.U(32.W))

    when(io.addr.fire){
        data := cache_data
    }.elsewhen(io.AXI.r.fire){
        data := io.AXI.r.bits.data
    }

    io.data.bits.data := data

    io.AXI.ar.bits.addr := io.data.bits.addr

    when(io.AXI.r.fire){
        cache(io.data.bits.addr(index_width + offset_width - 1, offset_width)) := Cat(true.B, io.data.bits.addr(tag_width - 1, index_width + offset_width), io.AXI.r.bits.data)
    }
    

    if(Config.Simulate){
        val Catch = Module(new Icache_catch)
        Catch.io.Icache := io.addr.fire
        Catch.io.map_hit := map_hit
        Catch.io.cache_hit := cache_hit
    }

    // AXI ignore

    io.AXI.aw.valid := false.B
    io.AXI.aw.bits.addr := 0.U
    io.AXI.aw.bits.size := 0.U
    io.AXI.aw.bits.id    := 0.U
    io.AXI.aw.bits.len   := 0.U
    io.AXI.aw.bits.burst := 0.U
    io.AXI.aw.bits.lock  := 0.U
    io.AXI.aw.bits.cache := 0.U
    io.AXI.aw.bits.prot  := 0.U
    io.AXI.aw.bits.qos   := 0.U
    io.AXI.w.valid := false.B
    io.AXI.w.bits.data := 0.U
    io.AXI.w.bits.strb := 0.U
    io.AXI.w.bits.last  := 1.U
    io.AXI.b.ready := false.B

    io.AXI.ar.bits.size  := 2.U
    io.AXI.ar.bits.id    := 0.U
    io.AXI.ar.bits.len   := 0.U
    io.AXI.ar.bits.burst := 0.U
    io.AXI.ar.bits.lock  := 0.U
    io.AXI.ar.bits.cache := 0.U
    io.AXI.ar.bits.prot  := 0.U
    io.AXI.ar.bits.qos   := 0.U
}

class Icache_Test extends Module{

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

        val Icache = Module(new Icache(
            Config.Icache_Param.address, 
            Config.Icache_Param.block_size, 
            Config.Icache_Param.block_num
        ))

        Icache.io.AXI <> master

        Icache.io.addr.valid <> io.WBU_2_IFU.valid
        Icache.io.addr.ready <> io.WBU_2_IFU.ready
        Icache.io.addr.bits <> io.REG_2_IFU.Next_PC

        Icache.io.data.valid <> io.IFU_2_IDU.valid
        Icache.io.data.ready <> io.IFU_2_IDU.ready
        Icache.io.data.bits.addr <> io.IFU_2_IDU.bits.PC
        Icache.io.data.bits.data <> io.IFU_2_IDU.bits.data

        io.IFU_2_REG.GPR_Aaddr := Icache.io.data.bits.data(19, 15)
        io.IFU_2_REG.GPR_Baddr := Icache.io.data.bits.data(24, 20)

        if(Config.Simulate){
            val Catch = Module(new IFU_catch)
            Catch.io.clock := clock
            Catch.io.valid := io.IFU_2_IDU.fire && !reset.asBool
            Catch.io.inst := io.IFU_2_IDU.bits.data
        }
    }
}
