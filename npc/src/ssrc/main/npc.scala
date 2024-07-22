package npc

import riscv_cpu._
import ram._

import chisel3._
import chisel3.util._
 
class npc extends Module {
  val io = IO(new Bundle {
    val inst_comp  = Output(Bool())
  })
  
  val CPU = Module(new CPU)
  val SRAM = Module(new SRAM(4.U))

  io.inst_comp   <> CPU.io.inst_comp

  SRAM.io.AXI <> CPU.io.AXI
}
