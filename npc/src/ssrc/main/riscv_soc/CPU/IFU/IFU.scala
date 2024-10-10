package riscv_cpu

import chisel3._
import chisel3.util._

import config._

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

class ysyx_23060198_IFU extends Module {
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(Flipped(new WBU_output)))
        val IFU_2_GNU = Decoupled(Output(new BUS_IFU_2_GNU))
        val IFU_2_REG = Output(new BUS_IFU_2_REG)
        val AXI = new AXI_Master
    })

    io.in.ready <> io.AXI.araddr.ready
    io.in.valid <> io.AXI.araddr.valid
    io.in.bits.addr <> io.AXI.araddr.bits.addr
    io.AXI.araddr.bits.size <> 2.U

    io.IFU_2_GNU.ready <> io.AXI.rdata.ready
    io.IFU_2_GNU.valid <> io.AXI.rdata.valid
    io.IFU_2_GNU.bits.data <> io.AXI.rdata.bits.data

    io.IFU_2_REG.GPR_Aaddr <> io.AXI.rdata.bits.data(19, 15)
    io.IFU_2_REG.GPR_Baddr <> io.AXI.rdata.bits.data(24, 20)

    io.AXI.awaddr.valid := false.B
    io.AXI.awaddr.bits.addr := 0.U
    io.AXI.awaddr.bits.size := 0.U
    io.AXI.wdata.valid := false.B
    io.AXI.wdata.bits.data := 0.U
    io.AXI.wdata.bits.strb := 0.U
    io.AXI.bresp.ready := false.B

    if(Config.DPIC_on){
        val trace = Module(new IFU_TRACE)

        trace.io.clock := clock
        trace.io.valid := io.IFU_2_GNU.valid && io.IFU_2_GNU.ready && !reset.asBool
        trace.io.addr := io.in.bits.addr
        trace.io.data := io.IFU_2_GNU.bits.data

        val IFU_PC = Module(new IFU_PC)
        IFU_PC.io.clock := clock
        IFU_PC.io.valid := io.IFU_2_GNU.valid && io.IFU_2_GNU.ready && !reset.asBool
    }
}
