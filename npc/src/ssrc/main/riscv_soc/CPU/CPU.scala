package riscv_cpu

import chisel3._
import chisel3.util._

import Instructions._
import signal_value._

class CPU() extends Module {
  val io = IO(new Bundle {
    val inst_input    = Flipped(Decoupled(UInt(32.W)))
    val pc_output     = Output(UInt(32.W))
    val mem_rdata     = Input(UInt(32.W))

    val mem_wdata     = Output(UInt(32.W))
    val mem_wop       = Output(MemOp_Type)
    val mem_wen       = Output(Bool())
    
    val mem_wraddr    = Output(UInt(32.W))
  })

  // Modules
  val GNU             = Module(new GNU()) // Generating Number Unit
  val EXU             = Module(new EXU()) // Execution Unit
  val WBU             = Module(new WBU()) // Write Back Unit
  val REG             = Module(new REG()) // Register File
  val BCU             = Module(new BCU()) // Branch Control Unit

  // wires
  val CSR_WADDRa      = Wire(UInt(12.W))
  val CSR_WADDRb      = Wire(UInt(12.W))
  val CSR_WDATAa      = Wire(UInt(32.W))
  val CSR_WDATAb      = Wire(UInt(32.W))
  val CSR_RADDR       = Wire(UInt(12.W))
  val CSR_RDATA       = Wire(UInt(32.W))

  val PCAsrc          = Wire(PCAsrc_Type)
  val PCBsrc          = Wire(PCBsrc_Type)

  // GNU Connections
  GNU.io.in.bits.inst <> io.inst_input.bits
  GNU.io.in.bits.PC   <> REG.io.pc_out
  GNU.io.in.bits.GPR_Adata <> REG.io.rdataa
  GNU.io.in.bits.GPR_Bdata <> REG.io.rdatab
  GNU.io.in.valid     <> io.inst_input.valid
  GNU.io.in.ready     <> io.inst_input.ready

  // EXU Connections
  EXU.io.in.RegWr        <> GNU.io.out.RegWr
  EXU.io.in.Branch       <> GNU.io.out.Branch
  EXU.io.in.MemtoReg     <> GNU.io.out.MemtoReg
  EXU.io.in.MemWr        <> GNU.io.out.MemWr
  EXU.io.in.MemOp        <> GNU.io.out.MemOp
  EXU.io.in.ALUAsrc      <> GNU.io.out.ALUAsrc
  EXU.io.in.ALUBsrc      <> GNU.io.out.ALUBsrc
  EXU.io.in.ALUctr       <> GNU.io.out.ALUctr
  EXU.io.in.csr_ctr      <> GNU.io.out.csr_ctr
  EXU.io.in.Imm          <> GNU.io.out.Imm
  EXU.io.in.GPR_Adata    <> GNU.io.out.GPR_Adata
  EXU.io.in.GPR_Bdata    <> GNU.io.out.GPR_Bdata
  EXU.io.in.GPR_waddr    <> GNU.io.out.GPR_waddr
  EXU.io.in.PC           <> GNU.io.out.PC
  EXU.io.in.CSR          <> CSR_RDATA

  // WBU Connections
  WBU.io.in.RegWr        <> EXU.io.out.RegWr
  WBU.io.in.Branch       <> EXU.io.out.Branch
  WBU.io.in.MemtoReg     <> EXU.io.out.MemtoReg
  WBU.io.in.MemWr        <> EXU.io.out.MemWr
  WBU.io.in.MemOp        <> EXU.io.out.MemOp
  WBU.io.in.csr_ctr      <> EXU.io.out.csr_ctr
  WBU.io.in.Imm          <> EXU.io.out.Imm
  WBU.io.in.GPR_Adata    <> EXU.io.out.GPR_Adata
  WBU.io.in.GPR_Bdata    <> EXU.io.out.GPR_Bdata
  WBU.io.in.GPR_waddr    <> EXU.io.out.GPR_waddr
  WBU.io.in.PC           <> EXU.io.out.PC
  WBU.io.in.CSR          <> CSR_RDATA
  WBU.io.in.Result       <> EXU.io.out.Result
  WBU.io.in.Zero         <> EXU.io.out.Zero
  WBU.io.in.Less         <> EXU.io.out.Less

  WBU.io.in.Mem_rdata    <> io.mem_rdata

  // REG Connections
  REG.io.wdata <> WBU.io.out.GPR_wdata
  REG.io.waddr <> WBU.io.out.GPR_waddr
  REG.io.wen   <> WBU.io.out.GPR_wen

  REG.io.raddra <> GNU.io.out.inst(19, 15)
  REG.io.raddrb <> GNU.io.out.inst(24, 20)
  REG.io.pc_in  <> WBU.io.out.Next_Pc

  when(GNU.io.out.csr_ctr === CSR_R1W0) {
    CSR_RADDR := "h341".U // instruction mret read mepc to recovered pc
  }.elsewhen(GNU.io.out.csr_ctr === CSR_R1W2) {
    CSR_RADDR := "h305".U // instruction ecall read mtevc to get to error order function
  }.otherwise {
    CSR_RADDR := GNU.io.out.Imm(11, 0)
  }

  when(GNU.io.out.csr_ctr === CSR_R1W2) {
    CSR_WADDRa := "h341".U // instruction ecall use csr mepc
  }.otherwise {
    CSR_WADDRa := GNU.io.out.Imm(11, 0)
  }

  when(GNU.io.out.csr_ctr === CSR_R1W2) {
    CSR_WDATAa := REG.io.pc_out // instruction ecall store current pc
  }.otherwise {
    CSR_WDATAa := EXU.io.out.GPR_Adata
  }

  CSR_WADDRb := "h342".U // instruction ecall write mstatus
  CSR_WDATAb := 11.U // for now, only set error status 11

  REG.io.csr_ctr    := WBU.io.out.CSR_ctr
  REG.io.csr_waddra := WBU.io.out.CSR_waddra
  REG.io.csr_waddrb := WBU.io.out.CSR_waddrb
  REG.io.csr_wdataa := WBU.io.out.CSR_wdataa
  REG.io.csr_wdatab := WBU.io.out.CSR_wdatab

  REG.io.csr_raddr := GNU.io.out.CSR_raddr

  CSR_RDATA := REG.io.csr_rdata

  // BCU Connections
  BCU.io.Branch <> GNU.io.out.Branch
  BCU.io.Zero   := EXU.io.out.Zero
  BCU.io.Less   := EXU.io.out.Less

  PCAsrc := BCU.io.PCAsrc
  PCBsrc := BCU.io.PCBsrc

  // Memory Connections
  io.mem_wraddr := EXU.io.out.Result
  io.mem_wdata := EXU.io.out.GPR_Bdata
  io.mem_wop   := GNU.io.out.MemOp
  io.mem_wen   := GNU.io.out.MemWr

  io.pc_output := REG.io.pc_out
}
