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

class GNU extends Module{
    val io = IO(new Bundle{
        val in       = Flipped(Decoupled(new GNU_input))
        // val inst_input= Flipped(Decoupled(UInt(32.W)))
        // val PC_input = Input(UInt(32.W))
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
    })

    io.in.inst.ready := true.B

    val inst = Wire(UInt(32.W))
    inst <> io.inst

    val idu = Module(new IDU)
    val igu = Module(new IGU)

    when(io.in.inst.valid) {
        inst := io.in.inst.bits
        io.Branch := idu.io.Branch
    }.otherwise {
        inst := NOP.U(32.W)
        io.Branch := Bran_NoC
    }

    idu.io.inst <> inst
    idu.io.RegWr <> io.RegWr
    idu.io.MemtoReg <> io.MemtoReg
    idu.io.MemWr <> io.MemWr
    idu.io.MemOp <> io.MemOp
    idu.io.ALUAsrc <> io.ALUAsrc
    idu.io.ALUBsrc <> io.ALUBsrc
    idu.io.ALUctr <> io.ALUctr
    idu.io.csr_ctr <> io.csr_ctr

    igu.io.inst <> inst
    igu.io.ExtOp <> idu.io.ExtOp
    igu.io.imm  <> io.Imm

    io.PC <> io.in.PC
}