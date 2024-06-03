package npc

import riscv_cpu._

import chisel3._
import chisel3.util._

class npc extends Module {
  val io = IO(new Bundle {
    val inst      = Flipped(Decoupled(UInt(32.W)))
    val mem_rdata = Input(UInt(32.W))
    val mem_raddr = Output(UInt(32.W))

    val mem_wdata = Output(UInt(32.W))
    val mem_wop   = Output(UInt(3.W))
    val mem_wen   = Output(Bool())
  })

  val IFU = Module(new IFU)

  IFU.io.inst_input <> io.inst

  val riscv_cpu = Module(new CPU)

  riscv_cpu.io.inst      <> IFU.io.inst_output
  riscv_cpu.io.mem_rdata := io.mem_rdata
  io.mem_raddr           := riscv_cpu.io.mem_raddr

  io.mem_wdata := riscv_cpu.io.mem_wdata
  io.mem_wop   := riscv_cpu.io.mem_wop
  io.mem_wen   := riscv_cpu.io.mem_wen
}
