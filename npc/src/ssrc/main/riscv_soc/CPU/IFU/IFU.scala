package riscv_cpu

import chisel3._
import chisel3.util._

import config._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

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
    })
    setInline("IFU_PC.v",
    """module IFU_PC(
    |    input clock,
    |    input valid
    |);
    |  import "DPI-C" function void IFU_finished();
    |  always @(posedge clock) begin
    |    if(valid) begin
    |      IFU_finished();
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

        io.WBU_2_IFU.ready <> AXI.ar.ready
        io.WBU_2_IFU.valid <> AXI.ar.valid
        io.REG_2_IFU.Next_PC <> AXI.ar.bits.addr
        AXI.ar.bits.size  := 2.U
        AXI.ar.bits.id    := 0.U
        AXI.ar.bits.len   := 0.U
        AXI.ar.bits.burst := 0.U
        AXI.ar.bits.lock  := 0.U
        AXI.ar.bits.cache := 0.U
        AXI.ar.bits.prot  := 0.U
        AXI.ar.bits.qos   := 0.U

        io.IFU_2_IDU.ready <> AXI.r.ready
        io.IFU_2_IDU.valid <> AXI.r.valid
        io.IFU_2_IDU.bits.data <> AXI.r.bits.data

        io.IFU_2_REG.GPR_Aaddr <> AXI.r.bits.data(19, 15)
        io.IFU_2_REG.GPR_Baddr <> AXI.r.bits.data(24, 20)

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

        if(Config.DPIC_on){
            val trace = Module(new IFU_TRACE)

            trace.io.clock := clock
            trace.io.valid := io.IFU_2_IDU.fire && !reset.asBool
            trace.io.addr := io.REG_2_IFU.Next_PC
            trace.io.data := io.IFU_2_IDU.bits.data

            val IFU_PC = Module(new IFU_PC)
            IFU_PC.io.clock := clock
            IFU_PC.io.valid := io.IFU_2_IDU.fire && !reset.asBool
        }
    }
}
