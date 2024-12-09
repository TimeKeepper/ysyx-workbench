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

class Icache(offsetWidth : Int, indexWidth : Int, tagWidth : Int, mapAddr : String) extends Module{
    val io = IO(new Bundle{
        val addr = Input(UInt(32.W))
        val data = Output(UInt(32.W))
        
        val cache_hit = Output(Bool())
        val wdata = Input(UInt(32.W))
        val wen = Input(Bool())
    })
    def mapBegin = offsetWidth + indexWidth + tagWidth
    def map = mapAddr.U(32.W)(31, mapBegin)
    def dataWidth = Math.pow(2, offsetWidth).toInt * 8
    def offsetPos = offsetWidth - 1
    def indexPos = offsetWidth + indexWidth - 1
    def tagPos = dataWidth + tagWidth - 1
    def dataPos = dataWidth - 1
    def validPos = tagPos + 1

    val icache = Mem(Math.pow(2, indexWidth).toInt, UInt((32 + tagWidth + 1).W))

    val index = io.addr(indexPos, offsetPos + 1)
    val tag = io.addr(31, indexPos + 1)
    val cache_tag = icache(index)(tagPos, dataPos + 1)
    io.data := icache(index)(dataPos, 0)

    val tag_hit = tag === Cat(map, cache_tag)
    val valid = icache(index)(validPos)
    io.cache_hit := valid && tag_hit

    when(io.wen){
        icache(index) := Cat(true.B, io.addr(24, 6), io.wdata)
    }
}

object Icache_state extends ChiselEnum{
    val idle, busy, get = Value
}

class Icache_output extends Bundle {
    val data = Output(UInt(32.W))
    val addr = Output(UInt(32.W))
}

class Icache_axi(address: Seq[AddressSet], block_size : Int, block_num : Int) extends Module {
    val io = IO(new Bundle{
        val AXI = AXI4Bundle(CPUAXI4BundleParameters())
        val addr = Flipped(Decoupled(Input(UInt(32.W))))
        val data = Decoupled(new Icache_output)
    })
    val offset_width = log2Ceil(block_size)
    val index_width = log2Ceil(block_num)
    
    val offset = io.addr.bits(offset_width - 1, 0)
    val index = io.addr.bits(index_width + offset_width - 1, offset_width)
    val tag = io.addr.bits(31, index_width + offset_width)

    val map_tag_width = log2Ceil(address.map(_.mask).reduce(_ max _))
    val cache_size = 1 + map_tag_width + (block_size * 8)
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
    io.data.bits.data := RegEnable(io.AXI.r.bits.data, io.AXI.r.fire)

    io.AXI.ar.bits.addr := io.data.bits.addr

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

        val Icache = Module(new Icache_axi(AddressSet.misaligned(0x80000000L, 0x8000000), 4, 32))

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

        // io.IFU_2_IDU.bits.PC := RegEnable(io.REG_2_IFU.Next_PC, io.WBU_2_IFU.fire)

        // val state = RegInit(bus_state.s_wait_valid)

        // val Icache = Module(new Icache(Config.Icache_Param.offsetWidth, Config.Icache_Param.indexWidth, Config.Icache_Param.tagWidth, Config.Icache_Param.mapAddr))
        // Icache.io.addr := Mux(io.WBU_2_IFU.fire, io.REG_2_IFU.Next_PC, io.IFU_2_IDU.bits.PC)
        
        // state := MuxLookup(state, bus_state.s_wait_valid)(
        //     Seq(
        //         bus_state.s_wait_valid -> Mux(io.WBU_2_IFU.valid, Mux(Icache.io.cache_hit, bus_state.s_wait_ready, bus_state.s_busy), bus_state.s_wait_valid),
        //         bus_state.s_busy -> Mux(AXI.r.fire, bus_state.s_wait_ready, bus_state.s_busy),
        //         bus_state.s_wait_ready -> Mux(io.IFU_2_IDU.ready, bus_state.s_wait_valid, bus_state.s_wait_ready)
        //     )
        // )

        // io.WBU_2_IFU.ready := state === bus_state.s_wait_valid
        // io.IFU_2_IDU.valid := state === bus_state.s_wait_ready

        // Icache.io.wen := state === bus_state.s_busy && AXI.r.fire
        // Icache.io.wdata := AXI.r.bits.data

        // AXI.aw.valid := false.B
        // AXI.aw.bits.addr := 0.U
        // AXI.aw.bits.size := 0.U
        // AXI.aw.bits.id    := 0.U
        // AXI.aw.bits.len   := 0.U
        // AXI.aw.bits.burst := 0.U
        // AXI.aw.bits.lock  := 0.U
        // AXI.aw.bits.cache := 0.U
        // AXI.aw.bits.prot  := 0.U
        // AXI.aw.bits.qos   := 0.U
        // AXI.w.valid := false.B
        // AXI.w.bits.data := 0.U
        // AXI.w.bits.strb := 0.U
        // AXI.w.bits.last  := 1.U
        // AXI.b.ready := false.B

        // AXI.ar.bits.size  := 2.U
        // AXI.ar.bits.id    := 0.U
        // AXI.ar.bits.len   := 0.U
        // AXI.ar.bits.burst := 0.U
        // AXI.ar.bits.lock  := 0.U
        // AXI.ar.bits.cache := 0.U
        // AXI.ar.bits.prot  := 0.U
        // AXI.ar.bits.qos   := 0.U

        // AXI.r.ready := io.IFU_2_IDU.ready

        // val axi_state = RegInit(bus_state.s_wait_ready)

        // axi_state := MuxLookup(axi_state, bus_state.s_wait_ready)(
        //     Seq(
        //         bus_state.s_wait_ready -> Mux(state === bus_state.s_busy && AXI.ar.ready, bus_state.s_wait_valid, bus_state.s_wait_ready),
        //         bus_state.s_wait_valid -> Mux(AXI.r.valid, bus_state.s_wait_ready, bus_state.s_wait_valid)
        //     )
        // )

        // AXI.ar.valid := state === bus_state.s_busy && axi_state === bus_state.s_wait_ready
        // AXI.ar.bits.addr := io.IFU_2_IDU.bits.PC

        // io.REG_2_IFU.Next_PC <> AXI.ar.bits.addr

        // io.IFU_2_REG.GPR_Aaddr <> io.IFU_2_IDU.bits.data(19, 15)
        // io.IFU_2_REG.GPR_Baddr <> io.IFU_2_IDU.bits.data(24, 20)

        // val inst = RegInit(0.U(32.W))

        // when(io.WBU_2_IFU.fire){
        //     inst := Icache.io.data
        // }.elsewhen(AXI.r.fire){
        //     inst := AXI.r.bits.data
        // }

        // io.IFU_2_IDU.bits.data := inst

        if(Config.Simulate){
            val Catch = Module(new IFU_catch)
            Catch.io.clock := clock
            Catch.io.valid := io.IFU_2_IDU.fire && !reset.asBool
            Catch.io.inst := io.IFU_2_IDU.bits.data
        }
    }
}
