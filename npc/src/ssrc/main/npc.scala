package npc

import riscv_cpu._
import ram._

import chisel3._
import chisel3.util._
 
class npc extends Module {
  val io = IO(new Bundle {
    val Dmem_rdata = Input(UInt(32.W))
    val Dmem_wraddr = Output(UInt(32.W))

    val Dmem_wdata = Output(UInt(32.W))
    val Dmem_wop   = Output(UInt(3.W))
    val Dmem_wen   = Output(Bool())

    val inst_comp  = Output(Bool())
  })
  
  val CPU = Module(new CPU)
  val SRAM = Module(new SRAM(4.U))

  io.Dmem_rdata  <> CPU.io.Dmem_rdata
  io.Dmem_wraddr <> CPU.io.Dmem_wraddr
  io.Dmem_wdata  <> CPU.io.Dmem_wdata
  io.Dmem_wop    <> CPU.io.Dmem_wop
  io.Dmem_wen    <> CPU.io.Dmem_wen
  io.inst_comp   <> CPU.io.inst_comp

  SRAM.io.araddr <> CPU.io.AXI_araddr
  SRAM.io.raddr  <> CPU.io.AXI_raddr
  SRAM.io.awaddr <> CPU.io.AXI_awaddr
  SRAM.io.wdata  <> CPU.io.AXI_wdata
  SRAM.io.bresp  <> CPU.io.AXI_bresp
}
