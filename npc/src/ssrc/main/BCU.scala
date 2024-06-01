package riscv_cpu

import chisel3._
import chisel3.util._

// riscv cpu branch control unit

class BCU extends Module {
    val io = IO(new Bundle {
        val Branch = Input(Bran_Type)
        val Zero   = Input(Bool())
        val Less   = Input(Bool())

        val PCAsrc = Output(Bool())
        val PCBsrc = Output(Bool())
    })

    when(io.Branch === Bran_Jmp || io.Branch === Bran_Jmpr) {
        io.PCAsrc := Y
    }.elsewhen(io.Branch === Bran_Jeq) {
        io.PCAsrc := io.Zero
    }.elsewhen(io.Branch === Bran_Jne) {
        io.PCAsrc := !io.Zero
    }.elsewhen(io.Branch === Bran_Jlt) {
        io.PCAsrc := io.Less
    }.elsewhen(io.Branch === Bran_Jge) {
        io.PCAsrc := !io.Less
    }.otherwise {
        io.PCAsrc := N
    }

    when(io.Branch === Bran_Jmpr) {
        io.PCBsrc := Y
    }.otherwise {
        io.PCBsrc := N
    }
}