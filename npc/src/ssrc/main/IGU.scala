package riscv_cpu

import chisel3._
import chisel3.util._
import scala.collection._
import javax.smartcardio.ATR

class IGU extends Module {
    val io = IO(new Bundle {
        val inst    = Input(UInt(32.W))
        val extop   = Input(UInt(3.W))

        val imm     = Output(UInt(32.W))
    })

    when(io.extop === 0.U) {
        io.imm := Cat(Fill(21, io.inst(31)), io.inst(31, 20))
    }.elsewhen(io.extop === 1.U) {
        io.imm := Cat(io.inst(31, 12), Fill(12, 0.U))
    }.elsewhen(io.extop === 2.U) {
        io.imm := Cat(Fill(20, io.inst(31)), io.inst(31, 25), io.inst(11, 7))
    }.elsewhen(io.extop === 3.U) {
        io.imm := Cat(Fill(20, io.inst(31)), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W))
    }.elsewhen(io.extop === 4.U) {
        io.imm := Cat(Fill(12, io.inst(31)), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W))
    }.otherwise {
        io.imm := 0.U(32.W)
    }
}