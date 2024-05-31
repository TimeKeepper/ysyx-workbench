package riscv_cpu

import chisel3._
import chisel3.util._
import scala.collection._
import javax.smartcardio.ATR

class BCU extends Module {
    val io = IO(new Bundle {
        val Branch = Input(UInt(3.W))
        val Less   = Input(Bool())
        val Zero   = Input(Bool())

        val PCAsrc = Output(Bool())
        val PCBsrc = Output(Bool())
    })

    when(io.Branch === 1.U || io.Branch === 2.U) {
        io.PCAsrc := true.B
    }.elsewhen(io.Branch === 4.U) {
        io.PCAsrc := io.Zero
    }.elsewhen(io.Branch === 5.U) {
        io.PCAsrc := !io.Zero
    }.elsewhen(io.Branch === 6.U) {
        io.PCAsrc := io.Less
    }.elsewhen(io.Branch === 7.U) {
        io.PCAsrc := !io.Less
    }.otherwise {
        io.PCAsrc := false.B
    }

    when(io.Branch === 2.U) {
        io.PCBsrc := true.B
    }.otherwise {
        io.PCBsrc := false.B
    }
}