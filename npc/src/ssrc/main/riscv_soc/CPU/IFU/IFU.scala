package riscv_cpu

import chisel3._
import chisel3.util._

class IFU_trace extends BlackBox{
    val io = IO(new Bundle {
        val clock = Input(Clock())
        val valid = Input(Bool())
        val addr  = Input(UInt(32.W))
        val data  = Input(UInt(32.W))
    })
}

//此模块将32为数据读取并根据memop处理数据，延迟不定周期后发送给IDU

class ysyx_23060198_IFU extends Module {
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(new IFU_input))
        val out = Decoupled(new IFU_Output)
        val AXI = new AXI_Master
    })

    io.in.ready <> io.AXI.araddr.ready
    io.in.valid <> io.AXI.araddr.valid
    io.in.bits.addr <> io.AXI.araddr.bits.addr
    io.AXI.araddr.bits.size <> 2.U

    io.out.ready <> io.AXI.rdata.ready
    io.out.valid <> io.AXI.rdata.valid
    io.out.bits.data <> io.AXI.rdata.bits.data

    io.AXI.awaddr.valid := false.B
    io.AXI.awaddr.bits.addr := 0.U
    io.AXI.awaddr.bits.size := 0.U
    io.AXI.wdata.valid := false.B
    io.AXI.wdata.bits.data := 0.U
    io.AXI.wdata.bits.strb := 0.U
    io.AXI.bresp.ready := false.B

    //此模块仅为调试用，可注释
    val trace = Module(new IFU_trace)

    trace.io.clock := clock
    trace.io.valid := io.out.valid && io.out.ready && !reset.asBool
    trace.io.addr := io.in.bits.addr
    trace.io.data := io.out.bits.data
}
