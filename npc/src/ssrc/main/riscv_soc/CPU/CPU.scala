package riscv_cpu

import chisel3._
import chisel3.util._
 
class CPU extends Module {
  val io = IO(new Bundle {
    val AXI = new AXI_Master

    val inst_comp  = Output(Bool())
  })
  
  val IFU             = Module(new IFU)
  val GNU             = Module(new GNU)
  val EXU             = Module(new EXU)
  val WBU             = Module(new WBU)
  val REG             = Module(new REG) 
  val AXI_Interconnect = Module(new AXI_Interconnect)

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
  AXI_Interconnect.io.IFU <> IFU.io.AXI
  AXI_Interconnect.io.LSU <> EXU.io.AXI

  AXI_Interconnect.io.ls_resq := IFU.io.out.valid
  AXI_Interconnect.io.if_resq := EXU.io.out.valid

  AXI_Interconnect.io.SRAM         <> io.AXI

  val comp_cache = RegInit(Bool(), false.B)
  comp_cache := WBU.io.out.valid
  when((comp_cache === false.B) && (WBU.io.out.valid === true.B)) {
    io.inst_comp := true.B
  }.otherwise {
    io.inst_comp := false.B
  }
}
