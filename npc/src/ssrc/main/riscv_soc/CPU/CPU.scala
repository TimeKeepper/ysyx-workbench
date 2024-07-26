package riscv_cpu

import chisel3._
import chisel3.util._
 
class ysyx_23060198 extends Module {
  val io = IO(new Bundle {
    val Master = new FIX_AXI_BUS

    val inst_comp  = Output(Bool())
  })
  
  val IFU             = Module(new ysyx_23060198_IFU)
  val GNU             = Module(new ysyx_23060198_GNU)
  val EXU             = Module(new ysyx_23060198_EXU)
  val WBU             = Module(new ysyx_23060198_WBU)
  val REG             = Module(new ysyx_23060198_REG) 
  val AXI_Interconnect = Module(new ysyx_23060198_AXI_Interconnect)

  // bus IFU -> GNU
  IFU.io.out.ready     <> GNU.io.in.ready
  IFU.io.out.valid     <> GNU.io.in.valid
  IFU.io.out.bits.data <> GNU.io.in.bits.IFU_io.data

  // bus IFU -> REG -> GNU without delay
  IFU.io.out.bits.data(19, 15) <> REG.io.in.GPR_raddra 
  IFU.io.out.bits.data(24, 20) <> REG.io.in.GPR_raddrb 
  REG.io.out.pc         <> GNU.io.in.bits.PC
  REG.io.out.GPR_rdataa <> GNU.io.in.bits.GPR_Adata
  REG.io.out.GPR_rdatab <> GNU.io.in.bits.GPR_Bdata

  // bus GNU -> EXU
  GNU.io.out.valid     <> EXU.io.in.valid
  GNU.io.out.ready     <> EXU.io.in.ready
  GNU.io.out.bits.GNU_io    <> EXU.io.in.bits.GNU_io     

  // bus GNU -> REG -> EXU without delay
  GNU.io.out.bits.CSR_raddr <> REG.io.in.csr_raddr  
  REG.io.out.csr_rdata      <> EXU.io.in.bits.CSR   

  // bus EXU -> WBU
  EXU.io.out.valid          <> WBU.io.in.valid
  EXU.io.out.ready          <> WBU.io.in.ready
  EXU.io.out.bits.EXU_io    <> WBU.io.in.bits.EXU_io    

  // bus WBU -> REG -> WBU with delay
  WBU.io.out.bits.WBU_io <> REG.io.in.WBU_io

  WBU.io.out.valid        <> IFU.io.in.valid     
  WBU.io.out.ready        <> IFU.io.in.ready     
  REG.io.out.pc           <> IFU.io.in.bits.addr 

  // bus AXI Interconnect
  io.Master.awready <> AXI_Interconnect.io.AXI.awaddr.ready
  io.Master.awvalid <> AXI_Interconnect.io.AXI.awaddr.valid
  io.Master.awaddr  <> AXI_Interconnect.io.AXI.awaddr.bits.addr
  io.Master.awid    := 0.U
  io.Master.awlen   := 0.U
  io.Master.awsize  := 0.U
  io.Master.awburst := 0.U

  io.Master.wready <> AXI_Interconnect.io.AXI.wdata.ready
  io.Master.wvalid <> AXI_Interconnect.io.AXI.wdata.valid
  io.Master.wdata  <> AXI_Interconnect.io.AXI.wdata.bits.data
  io.Master.wstrb   <> AXI_Interconnect.io.AXI.wdata.bits.strb
  io.Master.wlast         := 0.U

  io.Master.bready <> AXI_Interconnect.io.AXI.bresp.ready
  io.Master.bvalid <> AXI_Interconnect.io.AXI.bresp.valid
  io.Master.bresp  <> AXI_Interconnect.io.AXI.bresp.bits.bresp
  // io.Master.bid    

  io.Master.arready <> AXI_Interconnect.io.AXI.araddr.ready
  io.Master.arvalid <> AXI_Interconnect.io.AXI.araddr.valid
  io.Master.araddr  <> AXI_Interconnect.io.AXI.araddr.bits.addr
  io.Master.arid    := 0.U
  io.Master.arlen   := 0.U
  io.Master.arsize  := 0.U
  io.Master.arburst := 0.U

  io.Master.rready <> AXI_Interconnect.io.AXI.rdata.ready
  io.Master.rvalid <> AXI_Interconnect.io.AXI.rdata.valid
  io.Master.rresp  <> AXI_Interconnect.io.AXI.rdata.bits.resp
  io.Master.rdata  <> AXI_Interconnect.io.AXI.rdata.bits.data

  AXI_Interconnect.io.ls_resq := IFU.io.out.valid
  AXI_Interconnect.io.if_resq := EXU.io.out.valid

  AXI_Interconnect.io.IFU         <> IFU.io.AXI
  AXI_Interconnect.io.LSU         <> EXU.io.AXI

  val comp_cache = RegInit(Bool(), false.B)
  comp_cache := WBU.io.out.valid
  when((comp_cache === false.B) && (WBU.io.out.valid === true.B)) {
    io.inst_comp := true.B
  }.otherwise {
    io.inst_comp := false.B
  }
}
