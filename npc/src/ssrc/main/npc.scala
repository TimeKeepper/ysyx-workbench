package npc

import riscv_cpu._
import sram._

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

  val sram = Module(new SRAM)
  sram.io.inst_input <> io.inst

  val riscv_cpu = Module(new CPU)

  riscv_cpu.io.inst_input <> sram.io.inst_output
  riscv_cpu.io.mem_rdata  <> io.mem_rdata
  riscv_cpu.io.mem_wraddr <> io.mem_wraddr

  riscv_cpu.io.mem_wdata  <> io.mem_wdata
  riscv_cpu.io.mem_wop    <> io.mem_wop
  riscv_cpu.io.mem_wen    <> io.mem_wen
}
