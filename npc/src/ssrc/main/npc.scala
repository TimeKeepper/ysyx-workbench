package npc

import riscv_cpu._

import chisel3._
import chisel3.util._

class npc extends Module {
  val io = IO(new Bundle {
    val inst      = Flipped(Decoupled(UInt(32.W)))
    val mem_rdata = Input(UInt(32.W))
    val mem_wraddr = Output(UInt(32.W))

    val mem_wdata = Output(UInt(32.W))
    val mem_wop   = Output(UInt(3.W))
    val mem_wen   = Output(Bool())
  })

  val riscv_cpu = Module(new CPU)

  riscv_cpu.io.inst_input <> io.inst
  io.mem_wraddr            := riscv_cpu.io.mem_wraddr

  io.mem_wdata := riscv_cpu.io.mem_wdata
  io.mem_wop   := riscv_cpu.io.mem_wop
  io.mem_wen   := riscv_cpu.io.mem_wen
}
