package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import Instructions._
// riscv generating number(all meassge ALU and other thing needs) unit

class GNU_input extends Bundle{
    val inst = Input(UInt(32.W))
    val PC   = Input(UInt(32.W))
}

class GNU_output extends Bundle{
    val inst     = Output(UInt(32.W))
    val RegWr    = Output(Bool())
    val Branch   = Output(Bran_Type)
    val MemtoReg = Output(Bool())
    val MemWr    = Output(Bool())
    val MemOp    = Output(MemOp_Type)
    val ALUAsrc  = Output(ALUAsrc_Type)
    val ALUBsrc  = Output(ALUBSrc_Type)
    val ALUctr   = Output(ALUctr_Type)
    val csr_ctr  = Output(CSR_Type)
    val Imm      = Output(UInt(32.W))
    val PC       = Output(UInt(32.W))
}

class GNU extends Module{
    val io = IO(new Bundle{
        val in       = Flipped(Decoupled(new GNU_input))
        val out      = new GNU_output
        // val inst     = Output(UInt(32.W))
        // val RegWr    = Output(Bool())
        // val Branch   = Output(Bran_Type)
        // val MemtoReg = Output(Bool())
        // val MemWr    = Output(Bool())
        // val MemOp    = Output(MemOp_Type)
        // val ALUAsrc  = Output(ALUAsrc_Type)
        // val ALUBsrc  = Output(ALUBSrc_Type)
        // val ALUctr   = Output(ALUctr_Type)
        // val csr_ctr  = Output(CSR_Type)
        // val Imm      = Output(UInt(32.W))
        // val PC       = Output(UInt(32.W))
    })

    io.in.ready := true.B

    val inst = Wire(UInt(32.W))
    inst <> io.out.inst

    val idu = Module(new IDU)
    val igu = Module(new IGU)

    when(io.in.valid) {
        inst := io.in.bits.inst
        io.Branch := idu.io.Branch
    }.otherwise {
        inst := NOP.U(32.W)
        io.Branch := Bran_NoC
    }

    idu.io.inst <> inst
    idu.io.RegWr <> io.out.RegWr
    idu.io.MemtoReg <> io.out.MemtoReg
    idu.io.MemWr <> io.out.MemWr
    idu.io.MemOp <> io.out.MemOp
    idu.io.ALUAsrc <> io.out.ALUAsrc
    idu.io.ALUBsrc <> io.out.LUBsrc
    idu.io.ALUctr <> io.out.ALUctr
    idu.io.csr_ctr <> io.out.csr_ctr

    igu.io.inst <> inst
    igu.io.ExtOp <> idu.io.ExtOp
    igu.io.imm  <> io.out.Imm

    io.out.PC <> io.in.bits.PC
}