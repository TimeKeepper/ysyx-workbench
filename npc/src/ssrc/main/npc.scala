package npc

import riscv_cpu._

import chisel3._
import chisel3.util._

class npc extends Module {
    val io = IO(new Bundle {
        val inst = Input(UInt(32.W))
        val mem_rdata = Input(UInt(32.W))
        val mem_raddr = Output(UInt(32.W))

        val mem_wdata = Output(UInt(32.W))
        val mem_wop = Output(MemOp_Type)
        val mem_wen = Output(Bool())
    })

    val riscv_cpu = Module(new CPU)

    CPU.io.inst := io.inst
    CPU.io.mem_rdata := io.mem_rdata
    io.mem_raddr := CPU.io.mem_raddr

    io.mem_wdata := CPU.io.mem_wdata
    io.mem_wop := CPU.io.mem_wop
    io.mem_wen := CPU.io.mem_wen
}