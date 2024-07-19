package npc

import riscv_cpu._
import ram._

import chisel3._
import chisel3.util._
 
class npc extends Module {
  val io = IO(new Bundle {
    val AXI_araddr = Decoupled(new araddr)
    val AXI_raddr = Flipped(Decoupled(new raddr))
    val Dmem_rdata = Input(UInt(32.W))
    val Dmem_wraddr = Output(UInt(32.W))

    val Dmem_wdata = Output(UInt(32.W))
    val Dmem_wop   = Output(UInt(3.W))
    val Dmem_wen   = Output(Bool())

    val inst_comp  = Output(Bool())
  })
  
  val CPU = Module(new CPU)

  io.AXI_araddr  <> CPU.io.AXI_araddr
  io.AXI_raddr   <> CPU.io.AXI_raddr
  io.Dmem_rdata  <> CPU.io.Dmem_rdata
  io.Dmem_wraddr <> CPU.io.Dmem_wraddr
  io.Dmem_wdata  <> CPU.io.Dmem_wdata
  io.Dmem_wop    <> CPU.io.Dmem_wop
  io.Dmem_wen    <> CPU.io.Dmem_wen
  io.inst_comp   <> CPU.io.inst_comp
}
