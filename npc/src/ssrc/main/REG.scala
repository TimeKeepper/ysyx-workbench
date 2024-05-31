package riscv_cpu

import chisel3._
import chisel3.util._
import scala.collection._
import javax.smartcardio.ATR

// riscv cpu register file

class REG extends Module {
    val io = IO(new Bundle {
        val wdata = Input(UInt(32.W))
        val waddr = Input(UInt(5.W))
        val wen   = Input(Bool())

        val raddra = Input(UInt(5.W))
        val raddrb = Input(UInt(5.W))
        val rdataa  = Output(UInt(32.W))
        val rdatab  = Output(UInt(32.W))
    })

    val regs = Wire(Vec(32, UInt(32.W)), 0.U)

    when(io.wen && io.waddr =/= 0.U) {
        regs(io.waddr) := io.wdata
    }

    io.rdataa := regs(io.raddra)
    io.raddrb := regs(io.raddrb)
}