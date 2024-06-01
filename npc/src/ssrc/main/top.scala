package top

import chisel3._
import riscv_cpu._

class BlackBoxDPIC extends BlackBox {
  val io = IO(new Bundle{
    val inst = Input(UInt(32.W))
  })
}

class top extends Module {
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))
    val mem_rdata = Input(UInt(32.W))

    val mem_op = Output(UInt(3.W))
    val mem_wdata = Output(UInt(32.W))
    val mem_wen = Output(Bool())
    val mem_addr = Output(UInt(32.W))
  })
  
  val dpic = Module(new BlackBoxDPIC)
  dpic.io.inst := io.inst

  val riscv_cpu = Module(new CPU)

  riscv_cpu.io.inst := io.inst
  riscv_cpu.io.mem_rdata := io.mem_rdata

  io.mem_op := riscv_cpu.io.mem_op
  io.mem_wdata := riscv_cpu.io.mem_wdata
  io.mem_wen := riscv_cpu.io.mem_wen
  io.mem_addr := riscv_cpu.io.mem_addr
}
