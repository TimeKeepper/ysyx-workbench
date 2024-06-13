package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

// riscv excution unit

class EXU_input extends Bundle{
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
}

class EXU_output extends Bundle{
    val RegWr       = Output(Bool())
    val Branch      = Output(Bran_Type)
    val MemtoReg    = Output(Bool())
    val MemWr       = Output(Bool())
    val MemOp       = Output(MemOp_Type)
    val csr_ctr     = Output(CSR_Type)
    val PC          = Output(UInt(32.W))
    val CSR         = Output(UInt(32.W))
    val Result      = Output(UInt(32.W))
    val Zero        = Output(Bool())
    val Less        = Output(Bool())
}

class EXU extends Module {
    val io = IO(new Bundle{
        val in  = new EXU_input
        val out = new EXU_output
    })

    val alu = Module(new ALU)

    io.out.RegWr    <> io.in.RegWr
    io.out.Branch   <> io.in.Branch
    io.out.MemtoReg <> io.in.MemtoReg
    io.out.MemWr    <> io.in.MemWr
    io.out.MemOp    <> io.in.MemOp
    io.out.csr_ctr  <> io.in.csr_ctr
    io.out.PC       <> io.in.PC
    io.out.CSR      <> io.in.CSR
    
    io.out.Result   <> alu.io.ALUout 
    io.out.Zero     <> alu.io.Zero   
    io.out.Less     <> alu.io.Less  

    alu.io.ALUctr <> io.in.ALUctr

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