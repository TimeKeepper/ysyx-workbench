package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._


class IFU_input extends Bundle{
    val addr = Input(UInt(32.W))
}

class IFU_Output extends Bundle{
    val data = Output(UInt(32.W))
}

class GNU_Output extends Bundle{
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
}

class EXU_output extends Bundle{
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
}

class WBU_output extends Bundle{
    val inst_valid= Output(Bool())
    val Next_Pc   = Output(UInt(32.W))
    val GPR_waddr = Output(UInt(5.W))
    val GPR_wdata = Output(UInt(32.W))
    val GPR_wen   = Output(Bool())
    val CSR_ctr   = Output(CSR_Type)
    val CSR_waddra= Output(UInt(12.W))
    val CSR_waddrb= Output(UInt(12.W))
    val CSR_wdataa= Output(UInt(32.W))
    val CSR_wdatab= Output(UInt(32.W))
}

class araddr extends Bundle{
    val addr = Output(UInt(32.W))
}

class raddr extends Bundle{
    val data = Input(UInt(32.W))
    val resp = Input(Bool())
}

class awaddr extends Bundle{
    val addr = Output(UInt(32.W))
}

class wdata extends Bundle{
    val data = Output(UInt(32.W))
    val strb = Output(UInt(4.W))
}

class bresp extends Bundle{
    val bresp = Input(Bool())
}

class AXI_Master extends Bundle{
    val araddr = Decoupled(new araddr)
    val raddr = Flipped(Decoupled(new raddr))
    val awaddr = Decoupled(new awaddr)
    val wdata = Decoupled(new wdata)
    val bresp  = Flipped(Decoupled(new bresp))
}

class AXI_Slave extends Bundle{
    val araddr = Flipped(Decoupled(new araddr))
    val raddr = Decoupled(new raddr)
    val awaddr = Flipped(Decoupled(new awaddr))
    val wdata = Flipped(Decoupled(new wdata))
    val bresp  = Decoupled(new bresp)
}
