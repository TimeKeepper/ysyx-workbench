package riscv_cpu

import chisel3._
import chisel3.util._

class IFU_input extends Bundle{
    val pc = Input(UInt(32.W))
    val inst = Input(UInt(32.W))
}

class IFU_output extends Bundle{
    val pc = Output(UInt(32.W))
    val inst = Output(UInt(32.W))
}

class IFU extends Module{
    val io = IO(new Bundle{
        val in = new IFU_input
        val out = new IFU_output
    })

    io.out.pc := io.in.pc
    io.out.inst := io.in.inst
}