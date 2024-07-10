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
    val GPR_waddr = Input(UInt(5.W))
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
    val Imm         = Output(UInt(32.W))
    val GPR_Adata   = Output(UInt(32.W))
    val GPR_Bdata   = Output(UInt(32.W))
    val GPR_waddr   = Output(UInt(5.W))
    val PC          = Output(UInt(32.W))
    val CSR         = Output(UInt(32.W))
    val Result      = Output(UInt(32.W))
    val Zero        = Output(Bool())
    val Less        = Output(Bool())
}

class EXU extends Module {
    val io = IO(new Bundle{
        val in  = Flipped(Decoupled(new EXU_input))
        val out = Decoupled(new EXU_output)
    })

    val alu = Module(new ALU)

    io.out.bits.RegWr    <> io.in.bits.RegWr
    io.out.bits.Branch   <> io.in.bits.Branch
    io.out.bits.MemtoReg <> io.in.bits.MemtoReg
    io.out.bits.MemWr    <> io.in.bits.MemWr
    io.out.bits.MemOp    <> io.in.bits.MemOp
    io.out.bits.csr_ctr  <> io.in.bits.csr_ctr
    io.out.bits.Imm      <> io.in.bits.Imm
    io.out.bits.GPR_Adata<> io.in.bits.GPR_Adata
    io.out.bits.GPR_Bdata<> io.in.bits.GPR_Bdata
    io.out.bits.GPR_waddr<> io.in.bits.GPR_waddr
    io.out.bits.PC       <> io.in.bits.PC
    io.out.bits.CSR      <> io.in.bits.CSR

    io.out.Result   <> alu.io.ALUout 
    io.out.Zero     <> alu.io.Zero   
    io.out.Less     <> alu.io.Less  

    alu.io.ALUctr <> io.in.ALUctr

    alu.io.src_A := MuxLookup(io.in.ALUAsrc, 0.U)(Seq(
        ALUAsrc_RS1 -> io.in.GPR_Adata,
        ALUAsrc_PC  -> io.in.PC,
        ALUAsrc_CSR -> io.in.CSR,
    ))

    alu.io.src_B := MuxLookup(io.in.ALUBsrc, 0.U)(Seq(
        ALUBSrc_RS2 -> io.in.GPR_Bdata,
        ALUBSrc_IMM -> io.in.Imm,
        ALUBSrc_4   -> 4.U,
        ALUBSrc_RS1 -> io.in.GPR_Adata,
    ))
}