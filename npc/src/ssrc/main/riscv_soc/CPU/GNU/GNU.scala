package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import Instructions._
// riscv generating number(all meassge ALU and other thing needs) unit

class GNU_input extends Bundle{
    val inst = Input(UInt(32.W))
    val GPR_Adata = Input(UInt(32.W))
    val GPR_Bdata = Input(UInt(32.W))
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
    val GPR_Adata = Output(UInt(32.W))
    val GPR_Bdata = Output(UInt(32.W))
    val GPR_waddr = Output(UInt(5.W))
    val PC       = Output(UInt(32.W))
    val CSR_raddr= Output(UInt(12.W))
}

class GNU extends Module{
    val io = IO(new Bundle{
        val in       = Flipped(Decoupled(new GNU_input))
        val out      = new GNU_output
    })

    io.in.ready := true.B

    val idu = Module(new IDU)
    val igu = Module(new IGU)

    idu.io.inst     <> Mux(io.in.valid, io.in.bits.inst, NOP.U)
    idu.io.RegWr    <> io.out.RegWr
    io.out.Branch   <> Mux(io.in.valid, idu.io.Branch, Bran_NoC)
    idu.io.MemtoReg <> io.out.MemtoReg
    idu.io.MemWr    <> io.out.MemWr
    idu.io.MemOp    <> io.out.MemOp
    idu.io.ALUAsrc  <> io.out.ALUAsrc
    idu.io.ALUBsrc  <> io.out.ALUBsrc
    idu.io.ALUctr   <> io.out.ALUctr
    idu.io.csr_ctr  <> io.out.csr_ctr

    igu.io.inst     <> Mux(io.in.valid, io.in.bits.inst, NOP.U)
    igu.io.ExtOp    <> idu.io.ExtOp
    igu.io.imm      <> io.out.Imm

    io.out.GPR_Adata <> io.in.bits.GPR_Adata
    io.out.GPR_Bdata <> io.in.bits.GPR_Bdata
    io.out.GPR_waddr <> Mux(io.in.valid, io.in.bits.inst(11, 7), NOP.U(11, 7))
    io.out.PC       <> io.in.bits.PC
    io.out.inst     <> io.in.bits.inst
 
    io.out.CSR_raddr := MuxLookup(io.out.csr_ctr, io.out.Imm(11, 0))(Seq(
        CSR_R1W0 -> "h341".U,
        CSR_R1W2 -> "h305".U,
    ))
}