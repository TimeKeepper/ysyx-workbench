package npc

import riscv_cpu._
import ram._
import peripheral._

import chisel3._
import chisel3.util._
 
class npc extends Module {
  val io = IO(new Bundle {
    val inst_comp  = Output(Bool())
  })
  
  val CPU = Module(new CPU)
  val Xbar = Module(new Xbar)
  val SRAM = Module(new SRAM(4.U))
  val UART = Module(new UART)

  io.inst_comp   <> CPU.io.inst_comp

  Xbar.io.AXI <> CPU.io.AXI
  UART.io.AXI <> Xbar.io.UART
  SRAM.io.AXI <> Xbar.io.SRAM
}
