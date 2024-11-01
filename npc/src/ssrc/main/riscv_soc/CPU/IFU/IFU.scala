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

//此模块将32为数据读取并根据memop处理数据，延迟不定周期后发送给IDU

class ysyx_23060198_IFU(idBits: Int)(implicit p: Parameters) extends LazyModule {
  val masterNode = AXI4MasterNode(p(ExtIn).map(params =>
    AXI4MasterPortParameters(
      masters = Seq(AXI4MasterParameters(
        name = "cpu",
        id   = IdRange(0, 1 << idBits))))).toSeq)
    lazy val module = new Impl
    class Impl extends LazyModuleImp(this) {
        val io = IO(new Bundle{
            val WBU_2_IFU = Flipped(Decoupled(Input(new BUS_WBU_2_IFU)))
            val REG_2_IFU = Input(new BUS_REG_2_IFU)
            val IFU_2_IDU = Decoupled(Output(new BUS_IFU_2_IDU))
            val IFU_2_REG = Output(new BUS_IFU_2_REG)
            val AXI = AXI4Bundle(CPUAXI4BundleParameters())
        })

        io.WBU_2_IFU.ready <> io.AXI.ar.ready
        io.WBU_2_IFU.valid <> io.AXI.ar.valid
        io.REG_2_IFU.Next_PC <> io.AXI.ar.bits.addr
        io.AXI.ar.bits.size  <> 2.U
        io.AXI.ar.bits.id    := 0.U
        io.AXI.ar.bits.len   := 0.U
        io.AXI.ar.bits.burst := 0.U
        io.AXI.ar.bits.lock  := 0.U
        io.AXI.ar.bits.cache := 0.U
        io.AXI.ar.bits.prot  := 0.U
        io.AXI.ar.bits.qos   := 0.U

        io.IFU_2_IDU.ready <> io.AXI.r.ready
        io.IFU_2_IDU.valid <> io.AXI.r.valid
        io.IFU_2_IDU.bits.data <> io.AXI.r.bits.data

        io.IFU_2_REG.GPR_Aaddr <> io.AXI.r.bits.data(19, 15)
        io.IFU_2_REG.GPR_Baddr <> io.AXI.r.bits.data(24, 20)

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
        io.AXI.w.bits.last  := 0.U
        io.AXI.b.ready := false.B

        if(Config.DPIC_on){
            val trace = Module(new IFU_TRACE)

            trace.io.clock := clock
            trace.io.valid := io.IFU_2_IDU.valid && io.IFU_2_IDU.ready && !reset.asBool
            trace.io.addr := io.REG_2_IFU.Next_PC
            trace.io.data := io.IFU_2_IDU.bits.data

            val IFU_PC = Module(new IFU_PC)
            IFU_PC.io.clock := clock
            IFU_PC.io.valid := io.IFU_2_IDU.valid && io.IFU_2_IDU.ready && !reset.asBool
        }
    }
}
