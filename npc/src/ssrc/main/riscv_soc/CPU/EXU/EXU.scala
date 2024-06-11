package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

// riscv excution unit

class EXU extends Module {
    val io = IO(new Bundle{
        val RegWr     = Input(Bool())
        val Branch    = Input(Bran_Type)
        val MemtoReg  = Input(Bool())
        val MemWr     = Input(Bool())
        val MemOp     = Input(MemOp_Type)
        val ALUAsrc   = Input(ALUAsrc_Type)
        val ALUBsrc   = Input(ALUBSrc_Type)
        val ALUctr    = Input(ALUctr_Type)
        val csr_ctr   = Input(CSR_Type)
        val Imm       = Input(UInt(32.W))
        val GPR_Adata = Input(UInt(32.W))
        val GPR_Bdata = Input(UInt(32.W))
        val PC        = Input(UInt(32.W))
        val CSR       = Input(UInt(32.W))
        
        val RegWr_o    = Output(Bool())
        val Branch_o   = Output(Bran_Type)
        val MemtoReg_o = Output(Bool())
        val MemWr_o    = Output(Bool())
        val MemOp_o    = Output(MemOp_Type)
        val csr_ctr_o  = Output(CSR_Type)
        val PC_o       = Output(UInt(32.W))
        val CSR_o      = Output(UInt(32.W))
        val Result     = Output(UInt(32.W))
        val Zero       = Output(Bool())
        val Less       = Output(Bool())
    })

    io.RegWr_o    <> io.RegWr
    io.Branch_o   <> io.Branch
    io.MemtoReg_o <> io.MemtoReg
    io.MemWr_o    <> io.MemWr
    io.MemOp_o    <> io.MemOp
    io.csr_ctr_o  <> io.csr_ctr
    io.PC_o       <> io.PC
    io.CSR_o      <> io.CSR

    val alu = Module(new ALU)

    alu.io.ALUctr <> io.ALUctr
    alu.io.ALUout <> io.Result
    alu.io.Zero   <> io.Zero 
    alu.io.Less   <> io.Less

    alu.io.src_A := MuxLookup(io.ALUAsrc, 0.U)(Seq(
        A_RS1 -> io.GPR_Adata,
        A_PC  -> io.PC,
        A_CSR -> io.CSR,
    ))

    alu.io.src_B := MuxLookup(io.ALUBsrc, 0.U)(Seq(
        B_RS2 -> io.GPR_Bdata,
        B_IMM -> io.Imm,
        B_4   -> 4.U,
        B_RS1 -> io.GPR_Adata,
    ))
}