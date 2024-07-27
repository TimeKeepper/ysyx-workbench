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

class rdata extends Bundle{
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
    val rdata = Flipped(Decoupled(new rdata))
    val awaddr = Decoupled(new awaddr)
    val wdata = Decoupled(new wdata)
    val bresp  = Flipped(Decoupled(new bresp))
}

class AXI_Slave extends Bundle{
    val araddr = Flipped(Decoupled(new araddr))
    val rdata = Decoupled(new rdata)
    val awaddr = Flipped(Decoupled(new awaddr))
    val wdata = Flipped(Decoupled(new wdata))
    val bresp  = Decoupled(new bresp)
}
 
class FIX_AXI_BUS_Master extends Bundle{
  val awready = Input(Bool())
  val awvalid = Output(Bool())
  val awaddr  = Output(UInt(32.W))
  val awid    = Output(UInt(4.W))
  val awlen   = Output(UInt(8.W))
  val awsize  = Output(UInt(3.W))
  val awburst = Output(UInt(2.W))

  val wready = Input(Bool())
  val wvalid = Output(Bool())
  val wdata  = Output(UInt(64.W))
  val wstrb  = Output(UInt(8.W))
  val wlast  = Output(Bool())

  val bready = Output(Bool())
  val bvalid = Input(Bool())
  val bresp  = Input(UInt(2.W))
  val bid    = Input(UInt(4.W))

  val arready = Input(Bool())
  val arvalid = Output(Bool())
  val araddr  = Output(UInt(32.W))
  val arid    = Output(UInt(4.W))
  val arlen   = Output(UInt(8.W))
  val arsize  = Output(UInt(3.W))
  val arburst = Output(UInt(2.W))

  val rready = Output(Bool())
  val rvalid = Input(Bool())
  val rresp  = Input(UInt(2.W))
  val rdata  = Input(UInt(64.W))
  val rlast  = Input(Bool())
  val rid    = Input(UInt(4.W))
}

class FIX_AXI_BUS_Slave extends Bundle{
  val awready = Output(Bool())
  val awvalid = Input(Bool())
  val awaddr  = Input(UInt(32.W))
  val awid    = Input(UInt(4.W))
  val awlen   = Input(UInt(8.W))
  val awsize  = Input(UInt(3.W))
  val awburst = Input(UInt(2.W))

  val wready = Output(Bool())
  val wvalid = Input(Bool())
  val wdata  = Input(UInt(64.W))
  val wstrb  = Input(UInt(8.W))
  val wlast  = Input(Bool())

  val bready = Input(Bool())
  val bvalid = Output(Bool())
  val bresp  = Output(UInt(2.W))
  val bid    = Output(UInt(4.W))

  val arready = Output(Bool())
  val arvalid = Input(Bool())
  val araddr  = Input(UInt(32.W))
  val arid    = Input(UInt(4.W))
  val arlen   = Input(UInt(8.W))
  val arsize  = Input(UInt(3.W))
  val arburst = Input(UInt(2.W))

  val rready = Input(Bool())
  val rvalid = Output(Bool())
  val rresp  = Output(UInt(2.W))
  val rdata  = Output(UInt(64.W))
  val rlast  = Output(Bool())
  val rid    = Output(UInt(4.W))
}
