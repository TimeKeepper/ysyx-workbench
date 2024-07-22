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

class IFU extends Module {
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(new IFU_input))
        val out = Decoupled(new IFU_Output)
        val AXI = new AXI_Master
    })

    io.in.ready <> io.AXI.araddr.ready
    io.in.valid <> io.AXI.araddr.valid
    io.in.bits.addr <> io.AXI.araddr.bits.addr

    io.out.ready <> io.AXI.rdata.ready
    io.out.valid <> io.AXI.rdata.valid
    io.out.bits.data <> io.AXI.rdata.bits.data

    io.AXI.awaddr.valid := false.B
    io.AXI.awaddr.bits.addr := 0.U
    io.AXI.wdata.valid := false.B
    io.AXI.wdata.bits.data := 0.U
    io.AXI.wdata.bits.strb := 0.U
    io.AXI.bresp.ready := false.B

    //此模块仅为调试用，可注释
    val trace = Module(new IFU_trace)

    trace.io.clock := clock
    trace.io.valid := io.out.valid && io.out.ready
    trace.io.addr := io.in.bits.addr
    trace.io.data := io.out.bits.data
}

class Dcache_input extends Bundle{
    val addr = Input(UInt(32.W))
    val data = Input(UInt(32.W))
    val memop = Input(UInt(3.W))
    val memwen = Input(Bool())
}

class Dcache_output extends Bundle{
    val data = Output(UInt(32.W))
}

class Dcache extends Module {
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(new Dcache_input))
        val out = Decoupled(new Dcache_output)
    })

    val s_wait_valid :: s_wait_ready :: s_busy :: Nil = Enum(3)
    val state = RegInit(s_wait_valid)
    
    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.out.valid, s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.out.ready, s_wait_valid, s_wait_ready)
        )
    )

}