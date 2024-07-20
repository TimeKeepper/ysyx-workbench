package npc

import riscv_cpu._
import ram._

import chisel3._
import chisel3.util._
 
class CPU extends Module {
  val io = IO(new Bundle {
    val AXI_araddr = Decoupled(new araddr)
    val AXI_raddr = Flipped(Decoupled(new raddr))
    val AXI_awaddr = Decoupled(new awaddr)
    val AXI_wdata = Decoupled(new wdata)
    val AXI_bresp  = Flipped(Decoupled(new bresp))
    val Dmem_rdata = Input(UInt(32.W))
    val Dmem_wraddr = Output(UInt(32.W))

    val Dmem_wdata = Output(UInt(32.W))
    val Dmem_wop   = Output(UInt(3.W))
    val Dmem_wen   = Output(Bool())

    val inst_comp  = Output(Bool())
  })
  
  val IFU             = Module(new IFU)
  val GNU             = Module(new GNU)
  val EXU             = Module(new EXU)
  val LSU             = Module(new LSU)
  val WBU             = Module(new WBU)
  val REG             = Module(new REG) 

  // bus IFU AXI
  IFU.io.araddr         <> io.AXI_araddr
  IFU.io.raddr          <> io.AXI_raddr
  IFU.io.awaddr         <> io.AXI_awaddr
  IFU.io.wdata          <> io.AXI_wdata
  IFU.io.bresp          <> io.AXI_bresp

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
 
  // bus EXU -> LSU
  EXU.io.out.valid          <> LSU.io.in.valid
  EXU.io.out.ready          <> LSU.io.in.ready
  EXU.io.out.bits.EXU_io    <> LSU.io.in.bits.EXU_io    

  // bus LSU -> Dmem and Dmem -> LSU without delay
  io.Dmem_rdata                <> LSU.io.in.bits.Mem_rdata
  LSU.io.out.bits.Mem_wraddr   <> io.Dmem_wraddr
  LSU.io.out.bits.Mem_wdata    <> io.Dmem_wdata
  LSU.io.out.bits.MemOp        <> io.Dmem_wop
  LSU.io.out.bits.MemWr        <> io.Dmem_wen

  // bus LSU -> WBU
  LSU.io.out.valid          <> WBU.io.in.valid
  LSU.io.out.ready          <> WBU.io.in.ready
  LSU.io.out.bits.LSU_io    <> WBU.io.in.bits.LSU_io    

  // bus WBU -> REG -> WBU with delay
  WBU.io.out.bits.WBU_io <> REG.io.in.WBU_io

  WBU.io.out.valid        <> IFU.io.in.valid     
  WBU.io.out.ready        <> IFU.io.in.ready     
  REG.io.out.pc           <> IFU.io.in.bits.addr 

  val comp_cache = RegInit(Bool(), false.B)
  comp_cache := io.AXI_araddr.valid
  when((comp_cache === false.B) && (io.AXI_araddr.valid === true.B)) {
    io.inst_comp := true.B
  }.otherwise {
    io.inst_comp := false.B
  }
}
