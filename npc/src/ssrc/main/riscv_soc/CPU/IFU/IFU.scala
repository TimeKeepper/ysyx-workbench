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

class IFU_TRACE extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle {
        val clock = Input(Clock())
        val valid = Input(Bool())
        val addr  = Input(UInt(32.W))
        val data  = Input(UInt(32.W))
    })
    setInline("IFU_TRACE.v",
    """module IFU_TRACE(
    |    input clock,
    |    input valid,
    |    input [31:0] addr,
    |    input [31:0] data
    |);
    | import "DPI-C" function void check_special_inst(input int unsigned inst);
    | import "DPI-C" function void itrace_catch(input int unsigned addr, input int unsigned inst);
    | 
    | always @(posedge clock) begin
    |     if(valid) begin
    |         check_special_inst(data);
    |         itrace_catch(addr, data);
    |     end
    | end
    |
    |endmodule
    """.stripMargin)
}

class IFU_PC extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val valid = Input(Bool())
        val cache_hit = Input(Bool())
        val tag = Input(UInt(19.W))
        val cache_tah = Input(UInt(19.W))
    })
    setInline("IFU_PC.v",
    """module IFU_PC(
    |    input clock,
    |    input valid,
    |    input cache_hit,
    |    input [18:0] tag,
    |    input [18:0] cache_tah
    |);
    |  import "DPI-C" function void IFU_finished(input int unsigned cache_hit, input int unsigned tag, input int unsigned cache_tah);
    |  always @(posedge clock) begin
    |    if(valid) begin
    |      IFU_finished({31'h0, cache_hit}, {13'h0, tag}, {13'h0, cache_tah});
    |    end
    |  end
    |endmodule
    """.stripMargin)
}

class ysyx_23060198_IFU(idBits: Int)(implicit p: Parameters) extends LazyModule {
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
        val AXI = Wire(AXI4Bundle(CPUAXI4BundleParameters()))
        val (master, _) = masterNode.out(0)
        master <> AXI

        io.IFU_2_IDU.bits.PC := RegEnable(io.REG_2_IFU.Next_PC, io.WBU_2_IFU.fire)

        val state = RegInit(bus_state.s_wait_valid)

        val icache = Mem(16, UInt(52.W))

        val index = io.REG_2_IFU.Next_PC(5, 2)
        val tag = io.REG_2_IFU.Next_PC(31, 6)
        val cache_tag = icache(index)(50, 32)
        val data = icache(index)(31, 0)
        val tag_hit = tag === Cat("b1000000".U(7.W), cache_tag)
        val valid = icache(index)(51)
        val cache_hit = valid && tag_hit
        
        state := MuxLookup(state, bus_state.s_wait_valid)(
            Seq(
                bus_state.s_wait_valid -> Mux(io.WBU_2_IFU.valid, Mux(cache_hit, bus_state.s_wait_ready, bus_state.s_busy), bus_state.s_wait_valid),
                bus_state.s_busy -> Mux(AXI.r.fire, bus_state.s_wait_ready, bus_state.s_busy),
                bus_state.s_wait_ready -> Mux(io.IFU_2_IDU.ready, bus_state.s_wait_valid, bus_state.s_wait_ready)
            )
        )

        io.WBU_2_IFU.ready := state === bus_state.s_wait_valid
        io.IFU_2_IDU.valid := state === bus_state.s_wait_ready

        when(state === bus_state.s_busy && AXI.r.fire){
            icache(index) := Cat(1.U(1.W), io.IFU_2_IDU.bits.PC(24, 6), AXI.r.bits.data)
        }

        AXI.aw.valid := false.B
        AXI.aw.bits.addr := 0.U
        AXI.aw.bits.size := 0.U
        AXI.aw.bits.id    := 0.U
        AXI.aw.bits.len   := 0.U
        AXI.aw.bits.burst := 0.U
        AXI.aw.bits.lock  := 0.U
        AXI.aw.bits.cache := 0.U
        AXI.aw.bits.prot  := 0.U
        AXI.aw.bits.qos   := 0.U
        AXI.w.valid := false.B
        AXI.w.bits.data := 0.U
        AXI.w.bits.strb := 0.U
        AXI.w.bits.last  := 1.U
        AXI.b.ready := false.B

        AXI.ar.bits.size  := 2.U
        AXI.ar.bits.id    := 0.U
        AXI.ar.bits.len   := 0.U
        AXI.ar.bits.burst := 0.U
        AXI.ar.bits.lock  := 0.U
        AXI.ar.bits.cache := 0.U
        AXI.ar.bits.prot  := 0.U
        AXI.ar.bits.qos   := 0.U

        AXI.r.ready := io.IFU_2_IDU.ready

        val axi_state = RegInit(bus_state.s_wait_ready)

        axi_state := MuxLookup(axi_state, bus_state.s_wait_ready)(
            Seq(
                bus_state.s_wait_ready -> Mux(state === bus_state.s_busy && AXI.ar.ready, bus_state.s_wait_valid, bus_state.s_wait_ready),
                bus_state.s_wait_valid -> Mux(AXI.r.valid, bus_state.s_wait_ready, bus_state.s_wait_valid)
            )
        )

        AXI.ar.valid := state === bus_state.s_busy && axi_state === bus_state.s_wait_ready
        AXI.ar.bits.addr := io.IFU_2_IDU.bits.PC

        io.REG_2_IFU.Next_PC <> AXI.ar.bits.addr

        io.IFU_2_REG.GPR_Aaddr <> io.IFU_2_IDU.bits.data(19, 15)
        io.IFU_2_REG.GPR_Baddr <> io.IFU_2_IDU.bits.data(24, 20)

        val inst = RegInit(0.U(32.W))

        when(io.WBU_2_IFU.fire){
            inst := data
        }.elsewhen(AXI.r.fire){
            inst := AXI.r.bits.data
        }

        io.IFU_2_IDU.bits.data := inst

        if(Config.DPIC_on){
            val trace = Module(new IFU_TRACE)

            trace.io.clock := clock
            trace.io.valid := io.IFU_2_IDU.fire && !reset.asBool
            trace.io.addr := io.REG_2_IFU.Next_PC
            trace.io.data := io.IFU_2_IDU.bits.data

            val IFU_PC = Module(new IFU_PC)
            IFU_PC.io.clock := clock
            IFU_PC.io.valid := io.IFU_2_IDU.fire && !reset.asBool
            IFU_PC.io.cache_hit := cache_hit
            IFU_PC.io.tag := tag
            IFU_PC.io.cache_tah := cache_tag
        }
    }
}
