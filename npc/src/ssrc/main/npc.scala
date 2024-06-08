package npc

import riscv_cpu._

import chisel3._
import chisel3.util._

class npc extends Module {
  val io = IO(new Bundle {
    val inst      = Flipped(Decoupled(UInt(32.W)))
    val mem_rdata = Input(UInt(32.W))
    val mem_raddr = Output(UInt(32.W))

    // val mem_wdata = Output(UInt(32.W))
    // val mem_wop   = Output(UInt(3.W))
    // val mem_wen   = Output(Bool())
  })

  val I_mem = Mem(256, UInt(32.W))
  val D_mem = Mem(256, UInt(32.W))

  val riscv_cpu = Module(new CPU)

  riscv_cpu.io.inst_input.valid := true.B
  riscv_cpu.io.inst_input.bits := I_mem(riscv_cpu.io.pc_output)

  riscv_cpu.io.mem_rdata := D_mem(riscv_cpu.io.mem_raddr)
  D_mem(riscv_cpu.io.mem_raddr) := riscv_cpu.io.mem_wdata

  // riscv_cpu.io.inst_input <> io.inst
  // riscv_cpu.io.mem_rdata  := io.mem_rdata
  // io.mem_raddr            := riscv_cpu.io.mem_raddr

  // io.mem_wdata := riscv_cpu.io.mem_wdata
  // io.mem_wop   := riscv_cpu.io.mem_wop
  // io.mem_wen   := riscv_cpu.io.mem_wen
}
