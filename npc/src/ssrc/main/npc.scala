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

    val inst_comp  = Output(Bool())
  })
  
  val IFU             = Module(new IFU)
  val riscv_cpu       = Module(new CPU)
  val REG             = Module(new REG()) 

  IFU.io.in.bits.inst <> io.Imem_rdata.bits
  IFU.io.in.valid     <> riscv_cpu.io.Imem_raddr.valid
  IFU.io.in.ready     <> riscv_cpu.io.Imem_raddr.ready
  IFU.io.in.ready     <> io.Imem_rdata.ready
  IFU.io.in.bits.addr <> riscv_cpu.io.Imem_raddr.bits
  IFU.io.out.bits.inst  <> riscv_cpu.io.in.bits.inst
  IFU.io.out.bits.addr  <> riscv_cpu.io.in.bits.addr
  io.Imem_raddr  <> riscv_cpu.io.Imem_raddr.bits
  IFU.io.out.valid      <> riscv_cpu.io.in.valid
  IFU.io.out.ready      <> riscv_cpu.io.in.ready

  riscv_cpu.io.Dmem_rdata  <> io.Dmem_rdata
  riscv_cpu.io.Dmem_wraddr <> io.Dmem_wraddr

  riscv_cpu.io.Dmem_wdata  <> io.Dmem_wdata
  riscv_cpu.io.Dmem_wop    <> io.Dmem_wop
  riscv_cpu.io.Dmem_wen    <> io.Dmem_wen

  riscv_cpu.io.reg_in     <> REG.io.in
  riscv_cpu.io.reg_out    <> REG.io.out

  val comp_cache = RegInit(Bool(), false.B)
  comp_cache := IFU.io.in.ready
  when((comp_cache === false.B) && (IFU.io.in.ready === true.B)) {
    io.inst_comp := true.B
  }.otherwise {
    io.inst_comp := false.B
  }
}
