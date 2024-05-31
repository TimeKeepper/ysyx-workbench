package riscv_cpu

import chisel3._
import chisel3.util._

// riscv cpu register file

class REG extends Module {
    val io = IO(new Bundle {
        val value1        = Input(UInt(16.W))
        val value2        = Output(UInt(16.W))
        val raddr         = Input(UInt(5.W))
        val waddr         = Input(UInt(5.W))
        // val wdata = Input(UInt(32.W))
        // val waddr = Input(UInt(5.W))
        // val wen   = Input(Bool())

        // val raddra = Input(UInt(5.W))
        // val raddrb = Input(UInt(5.W))
        // val rdataa  = Output(UInt(32.W))
        // val rdatab  = Output(UInt(32.W))
    })

    val regs = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

    io.value2 := regs(io.raddr)

    regs(io.waddr) := io.value1

    // when(io.wen && io.waddr =/= 0.U) {
    //     regs(io.waddr) := io.wdata
    // }

    // io.rdataa := regs(io.raddra)
    // io.raddrb := regs(io.raddrb)
}