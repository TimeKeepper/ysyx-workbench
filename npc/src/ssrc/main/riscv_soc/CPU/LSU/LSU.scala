package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

// riscv load store unit

class LSU_input extends Bundle{
    val RegWr       = Input(Bool())
    val Branch      = Input(Bran_Type)
    val MemtoReg    = Input(Bool())
    val MemWr       = Input(Bool())
    val MemOp       = Input(MemOp_Type)
    val csr_ctr     = Input(CSR_Type)
    val Imm         = Input(UInt(32.W))
    val GPR_Adata   = Input(UInt(32.W))
    val GPR_Bdata   = Input(UInt(32.W))
    val GPR_waddr   = Input(UInt(5.W))
    val PC          = Input(UInt(32.W))
    val CSR         = Input(UInt(32.W))
    val Result      = Input(UInt(32.W))
    val Zero        = Input(Bool())
    val Less        = Input(Bool())

    val Mem_rdata   = Input(UInt(32.W))
}

class LSU_output extends Bundle{
    val RegWr       = Output(Bool())
    val Branch      = Output(Bran_Type)
    val MemtoReg    = Output(Bool())
    val csr_ctr     = Output(CSR_Type)
    val Imm         = Output(UInt(32.W))
    val GPR_Adata   = Output(UInt(32.W))
    val GPR_waddr   = Output(UInt(5.W))
    val PC          = Output(UInt(32.W))
    val CSR         = Output(UInt(32.W))
    val Result      = Output(UInt(32.W))
    val Zero        = Output(Bool())
    val Less        = Output(Bool())
    val Mem_rdata   = Output(UInt(32.W))

    val Mem_wraddr = Output(UInt(32.W))
    val Mem_wdata  = Output(UInt(32.W))
    val MemOp    = Output(MemOp_Type)
    val MemWr    = Output(Bool())
}

class LSU extends Module{
    val io = IO(new Bundle{
        val in = new LSU_input
        val out = new LSU_output
    })

    io.out.Mem_wraddr   <> io.in.Result
    io.out.Mem_wdata    <> io.in.GPR_Bdata
    io.out.MemOp        <> io.in.MemOp
    io.out.MemWr        <> io.in.MemWr
    io.out.Mem_rdata    <> io.in.Mem_rdata

    io.out.RegWr        <> io.in.RegWr     
    io.out.Branch       <> io.in.Branch    
    io.out.MemtoReg     <> io.in.MemtoReg  
    io.out.csr_ctr      <> io.in.csr_ctr   
    io.out.Imm          <> io.in.Imm       
    io.out.GPR_Adata    <> io.in.GPR_Adata 
    io.out.GPR_waddr    <> io.in.GPR_waddr 
    io.out.PC           <> io.in.PC        
    io.out.CSR          <> io.in.CSR       
    io.out.Result       <> io.in.Result    
    io.out.Zero         <> io.in.Zero      
    io.out.Less         <> io.in.Less      
}