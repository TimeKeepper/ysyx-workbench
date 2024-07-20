package riscv_cpu

import chisel3._
import chisel3.util._

//此模块将32为数据读取并根据memop处理数据，延迟不定周期后发送给IDU

class IFU extends Module {
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(new IFU_input))
        val out = Decoupled(new IFU_Output)
        val araddr = Decoupled(new araddr)
        val raddr = Flipped(Decoupled(new raddr))
        val awaddr = Decoupled(new awaddr)
        val wdata = Decoupled(new wdata)
        val bresp  = Flipped(Decoupled(new bresp))
    })

    io.in.ready <> io.araddr.ready
    io.in.valid <> io.araddr.valid
    io.in.bits.addr <> io.araddr.bits.addr

    io.out.ready <> io.raddr.ready
    io.out.valid <> io.raddr.valid
    io.out.bits.data <> io.raddr.bits.data

    io.awaddr.valid := false.B
    io.awaddr.bits.addr := 0.U
    io.wdata.valid := false.B
    io.wdata.bits.data := 0.U
    io.bresp.ready := false.B
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