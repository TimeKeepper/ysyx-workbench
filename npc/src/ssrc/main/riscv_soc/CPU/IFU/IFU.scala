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
        val input = new IFU_input
        val output = new IFU_output
    })

    io.output.pc := io.input.pc
    io.output.inst := io.input.inst
}