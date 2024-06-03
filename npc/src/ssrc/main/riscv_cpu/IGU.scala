package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

// riscv cpu immediate generation unit

class IGU extends Module {
  val io = IO(new Bundle {
    val inst  = Input(UInt(32.W))
    val Extop = Input(ExtOp_Type)

    val imm = Output(UInt(32.W))
  })

  val imm = MuxLookup(io.Extop, 0.U)(
    Seq(
      immI -> Cat(Fill(21, io.inst(31)), io.inst(31, 20))
      immU -> Cat(io.inst(31, 12), Fill(12, 0.U))
      immS -> Cat(Fill(20, io.inst(31)), io.inst(31, 25), io.inst(11, 7))
      immB -> Cat(Fill(20, io.inst(31)), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W))
      immJ -> Cat(Fill(12, io.inst(31)), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W))
    )
  )

  io.imm <> imm
}
