package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

// riscv cpu instrcution fetch unit

class IFU extends Module {
    val io = IO(new Bundle {
        val inst_input  = Flipped(Decoupled(UInt(32.W)))
        val inst_output = Decoupled(UInt(32.W))
    })

    io.inst_input.ready := true.B

    val s_idle :: s_wait_ready :: Nil = Enum(2)

    val state = RegInit(s_idle)
    state := MuxLookup(state, s_idle)(Seq(
        s_idle -> Mux(io.inst_output.valid, s_wait_ready, s_idle),
        s_wait_ready -> Mux(io.inst_output.ready, s_idle, s_wait_ready),
    ))

    io.inst_output.valid := io.inst_input.valid && io.inst_input.ready

    io.inst_output.bits := io.inst_input.bits
}
