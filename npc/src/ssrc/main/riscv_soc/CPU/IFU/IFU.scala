package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

// riscv instruction fetch unit

class IFU_input extends Bundle {
  val pc   = Input(UInt(32.W))
  val inst = Input(UInt(32.W))
}

class IFU_output extends Bundle {
  val pc   = Output(UInt(32.W))
  val inst = Output(UInt(32.W))
}

class IFU extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new IFU_input))
    val out = Decoupled(new IFU_output)
    val inst_done = Input(Bool())
  })

  val pc   = RegInit(0.U(32.W))
  val inst = RegInit(0.U(32.W))

  val s_wait_valid :: s_wait_ready :: Nil = Enum(2)
  val state                               = RegInit(s_wait_valid)

  // io.in.ready  := state === s_wait_valid
  io.out.valid := state === s_wait_ready

  state := MuxLookup(state, s_wait_ready)(
    Seq(
      s_wait_valid -> Mux(io.in.valid && io.inst_done, s_wait_ready, s_wait_valid),
      s_wait_ready -> Mux(io.out.ready, s_wait_valid, s_wait_ready)
    )
  )

  when(state === s_wait_valid) {
    pc   := io.in.bits.pc
    inst := io.in.bits.inst
  }

  io.out.bits.pc   := pc
  io.out.bits.inst := inst
}
