package npc

import riscv_cpu._
import ram._

import chisel3._
import chisel3.util._

class npc extends Module {
  val io = IO(new Bundle {
    val Imem_rdata = Flipped(Decoupled(UInt(32.W)))
    val Imem_raddr = Output(UInt(32.W))

    val Dmem_rdata = Input(UInt(32.W))
    val Dmem_wraddr = Output(UInt(32.W))

    val Dmem_wdata = Output(UInt(32.W))
    val Dmem_wop   = Output(UInt(3.W))
    val Dmem_wen   = Output(Bool())
  })
  
  val IFU = Module(new IFU)

  val riscv_cpu = Module(new CPU)

  IFU.io.in.bits.inst <> io.Imem_rdata.bits
  IFU.io.in.valid     <> io.Imem_rdata.valid
  IFU.io.in.ready     <> io.Imem_rdata.ready
  IFU.io.in.bits.addr <> riscv_cpu.io.Imem_raddr
  IFU.io.out.bits.inst  <> riscv_cpu.io.Imem_rdata.bits
  IFU.io.out.bits.addr  <> io.Imem_raddr
  IFU.io.out.valid      <> riscv_cpu.io.Imem_rdata.valid
  IFU.io.out.ready      <> riscv_cpu.io.Imem_rdata.ready

  riscv_cpu.io.Dmem_rdata  <> io.Dmem_rdata
  riscv_cpu.io.Dmem_wraddr <> io.Dmem_wraddr

  riscv_cpu.io.Dmem_wdata  <> io.Dmem_wdata
  riscv_cpu.io.Dmem_wop    <> io.Dmem_wop
  riscv_cpu.io.Dmem_wen    <> io.Dmem_wen
}
