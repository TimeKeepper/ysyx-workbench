package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

// riscv cpu branch control unit

class BCU extends Module {
    val io = IO(new Bundle {
        val Branch = Input(Bran_Type)
        val Zero   = Input(Bool())
        val Less   = Input(Bool())

        val PCAsrc = Output(PCAsrc_Type)
        val PCBsrc = Output(PCBsrc_Type)
    })

    when(io.Branch === Bran_Jmp || io.Branch === Bran_Jmpr) {
        io.PCAsrc := PCAsrc_Imm
    }.elsewhen(io.Branch === Bran_Jeq) {
        when(io.Zero) {
            io.PCAsrc := PCAsrc_Imm
        }.otherwise {
            io.PCAsrc := PCAsrc_4
        }
    }.elsewhen(io.Branch === Bran_Jne) {
        when(io.Zero) {
            io.PCAsrc := PCAsrc_4
        }.otherwise {
            io.PCAsrc := PCAsrc_Imm
        }
    }.elsewhen(io.Branch === Bran_Jlt) {
        when(io.Less) {
            io.PCAsrc := PCAsrc_Imm
        }.otherwise {
            io.PCAsrc := PCAsrc_4
        }
    }.elsewhen(io.Branch === Bran_Jge) {
        when(io.Less) {
            io.PCAsrc := PCAsrc_4
        }.otherwise {
            io.PCAsrc := PCAsrc_Imm
        }
    }.elsewhen(io.Branch === Bran_Jcsr) {
        io.PCAsrc := PCAsrc_csr
    }.otherwise {
        io.PCAsrc := PCAsrc_4
    }

    when(io.Branch === Bran_Jmpr) {
        io.PCBsrc := PCBsrc_gpr
    }.otherwise {
        io.PCBsrc := PCBsrc_pc
    }
}