package npc

import riscv_cpu._
import sram._

import chisel3._
import chisel3.util._

class npc extends Module {
  val io = IO(new Bundle {
    val Imem_rdata = Input(UInt(32.W))
    val Imem_raddr     = Output(UInt(32.W))
    val Dmem_rdata = Input(UInt(32.W))
    val Dmem_wraddr = Output(UInt(32.W))

    val Dmem_wdata = Output(UInt(32.W))
    val Dmem_wop   = Output(UInt(3.W))
    val Dmem_wen   = Output(Bool())
  })
  
  val riscv_cpu = Module(new CPU)

  riscv_cpu.io.Imem_rdata <> io.Imem_rdata
  riscv_cpu.io.Imem_raddr  <> io.Imem_raddr
  riscv_cpu.io.Dmem_rdata  <> io.Dmem_rdata
  riscv_cpu.io.Dmem_wraddr <> io.Dmem_wraddr

  riscv_cpu.io.Dmem_wdata  <> io.Dmem_wdata
  riscv_cpu.io.Dmem_wop    <> io.Dmem_wop
  riscv_cpu.io.Dmem_wen    <> io.Dmem_wen
}
