package top

import chisel3._
import riscv_cpu._

class BlackBoxDPIC extends BlackBox {
  val io = IO(new Bundle{
    val inst = Input(UInt(32.W))
    val gpr_10 = Input(UInt(32.W))
  })
}

class top extends Module {
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))
    val mem_rdata = Input(UInt(32.W))
    val mem_raddr = Output(UInt(32.W))

    val mem_wdata = Output(UInt(32.W))
    val mem_wop = Output(UInt(3.W))
    val mem_wen = Output(Bool())
  })
  
  val dpic = Module(new BlackBoxDPIC)
  dpic.io.inst := io.inst
  dpic.io.gpr_10 := riscv_cpu.REG.gpr(10)

  val riscv_cpu = Module(new CPU)

  riscv_cpu.io.inst := dpic.io.inst
  riscv_cpu.io.mem_rdata := io.mem_rdata
  io.mem_raddr := riscv_cpu.io.mem_raddr

  io.mem_wdata := riscv_cpu.io.mem_wdata
  io.mem_wop := riscv_cpu.io.mem_wop
  io.mem_wen := riscv_cpu.io.mem_wen
}
