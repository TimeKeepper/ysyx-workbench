package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._


class WBU_output extends Bundle{
    val addr = Output(UInt(32.W))
}

class BUS_IFU_2_IDU extends Bundle{
    val data = UInt(32.W)
}

class BUS_IFU_2_REG extends Bundle{
    val GPR_Aaddr  = UInt(5.W)
    val GPR_Baddr  = UInt(5.W)
}

class BUS_REG_2_IDU extends Bundle{
    val PC         = UInt(32.W)
    val CSR_rdata  = UInt(32.W)
    val GPR_Adata  = UInt(32.W)
    val GPR_Bdata  = UInt(32.W)
}

class BUS_IDU_2_EXU extends Bundle{
    val RegWr    = Bool()
    val Branch   = Bran_Type
    val MemtoReg = Bool()
    val MemWr    = Bool()
    val MemOp    = MemOp_Type
    val ALUAsrc  = ALUAsrc_Type
    val ALUBsrc  = ALUBSrc_Type
    val ALUctr   = ALUctr_Type
    val csr_ctr  = CSR_Type
    val Imm      = UInt(32.W)
    val GPR_Adata = UInt(32.W)
    val GPR_Bdata = UInt(32.W)
    val GPR_waddr = UInt(5.W)
    val PC       = UInt(32.W)
    val CSR_rdata = UInt(32.W)
}

class BUS_IDU_2_REG extends Bundle{
    val CSR_raddr   = UInt(12.W)
}

class BUS_REG_2_EXU extends Bundle{
}

class BUS_EXU_2_WBU extends Bundle{
    val inst_valid= Bool()
    val Next_Pc   = UInt(32.W)
    val GPR_waddr = UInt(5.W)
    val GPR_wdata = UInt(32.W)
    val GPR_wen   = Bool()
    val CSR_ctr   = CSR_Type
    val CSR_waddra= UInt(12.W)
    val CSR_waddrb= UInt(12.W)
    val CSR_wdataa= UInt(32.W)
    val CSR_wdatab= UInt(32.W)
}

class BUS_WBU_2_REG extends Bundle{
    val inst_valid= Bool()
    val Next_Pc   = UInt(32.W)
    val GPR_waddr = UInt(5.W)
    val GPR_wdata = UInt(32.W)
    val GPR_wen   = Bool()
    val CSR_ctr   = CSR_Type
    val CSR_waddra= UInt(12.W)
    val CSR_waddrb= UInt(12.W)
    val CSR_wdataa= UInt(32.W)
    val CSR_wdatab= UInt(32.W)
}

class BUS_WBU_2_IFU extends Bundle

class BUS_REG_2_IFU extends Bundle{
    val Next_PC = UInt(32.W)
}

trait Bus_default_value {
    def setDefault(): Unit
}

class araddr extends Bundle{
    val addr = Output(UInt(32.W))
    val size = Output(UInt(3.W))
}

class rdata extends Bundle{
    val data = Input(UInt(32.W))
    val resp = Input(Bool())
}

class awaddr extends Bundle{
    val addr = Output(UInt(32.W))
    val size = Output(UInt(3.W))
}

class wdata extends Bundle{
    val data = UInt(32.W)
    val strb = UInt(4.W)
}

class bresp extends Bundle{
    val bresp = Input(Bool())
}

class AXI_Master extends Bundle with Bus_default_value{
    val araddr = Decoupled(new araddr)
    val rdata = Flipped(Decoupled(new rdata))
    val awaddr = Decoupled(new awaddr)
    val wdata = Decoupled(Output(new wdata))
    val bresp  = Flipped(Decoupled(new bresp))

    def setDefault(): Unit = {
        araddr.ready := false.B
        rdata.valid := false.B
        awaddr.ready := false.B
        wdata.ready := false.B
        bresp.valid := false.B
    }
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
  val wdata  = Output(UInt(32.W))
  val wstrb  = Output(UInt(4.W))
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
  val rdata  = Input(UInt(32.W))
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
  val wdata  = Input(UInt(32.W))
  val wstrb  = Input(UInt(4.W))
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
  val rdata  = Output(UInt(32.W))
  val rlast  = Output(Bool())
  val rid    = Output(UInt(4.W))
}
