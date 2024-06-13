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
  val REG             = Module(new REG()) // Register File
  val BCU             = Module(new BCU()) // Branch Control Unit

  // wires
  val Next_PC         = Wire(UInt(32.W))
  val Cur_PC          = Wire(UInt(32.W))

  val GPR_WADDR       = Wire(UInt(5.W))
  val GPR_WDATA       = Wire(UInt(32.W))
  val GPR_RADDRa      = Wire(UInt(5.W))
  val GPR_RADDRb      = Wire(UInt(5.W))
  val GPR_RDATAa      = Wire(UInt(32.W))
  val GPR_RDATAb      = Wire(UInt(32.W))

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
  GNU.io.in.valid     <> io.inst_input.valid
  GNU.io.in.ready     <> io.inst_input.ready
  GNU.io.in.GPR_Adata <> GPR_RDATAa
  GNU.io.in.GPR_Bdata <> GPR_RDATAb
  GNU.io.in.bits.PC   <> Cur_PC

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
  EXU.io.in.GPR_Adata    <> GNU.io.in.GPR_Adata
  EXU.io.in.GPR_Bdata    <> GNU.io.in.GPR_Bdata
  EXU.io.in.PC           <> GNU.io.out.PC
  EXU.io.in.CSR          <> CSR_RDATA

  // REG Connections
  GPR_WADDR := GNU.io.out.inst(11, 7)

  when(GNU.io.out.MemtoReg) {
    GPR_WDATA := io.mem_rdata
  }.otherwise {
    GPR_WDATA := EXU.io.out.Result
  }

  GPR_WADDR := GNU.io.out.inst(11, 7)

  REG.io.wdata := GPR_WDATA
  REG.io.waddr := GPR_WADDR
  REG.io.wen   := GNU.io.out.RegWr

  GPR_RADDRa    := GNU.io.out.inst(19, 15)
  GPR_RADDRb    := GNU.io.out.inst(24, 20)
  REG.io.raddra := GPR_RADDRa
  REG.io.raddrb := GPR_RADDRb
  GPR_RDATAa    := REG.io.rdataa
  GPR_RDATAb    := REG.io.rdatab

  val PCAval = Wire(UInt(32.W))
  val PCBval = Wire(UInt(32.W))

  when(PCAsrc === PCAsrc_Imm) {
    PCAval := GNU.io.out.Imm
  }.elsewhen(PCAsrc === PCAsrc_4) {
    PCAval := 4.U
  }.elsewhen(PCAsrc === PCAsrc_0) {
    PCAval := 0.U
  }.otherwise {
    PCAval := CSR_RDATA
  }

  when(PCBsrc === PCBsrc_gpr) {
    PCBval := GPR_RDATAa
  }.elsewhen(PCBsrc === PCBsrc_pc) {
    PCBval := Cur_PC
  }.otherwise {
    PCBval := 0.U
  }

  Next_PC := PCAval + PCBval

  REG.io.pc_in := Next_PC
  Cur_PC       := REG.io.pc_out

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
    CSR_WDATAa := Cur_PC // instruction ecall store current pc
  }.otherwise {
    CSR_WDATAa := GPR_RDATAa
  }

  CSR_WADDRb := "h342".U // instruction ecall write mstatus
  CSR_WDATAb := 11.U // for now, only set error status 11

  REG.io.csr_ctr    := GNU.io.out.csr_ctr
  REG.io.csr_waddra := CSR_WADDRa
  REG.io.csr_waddrb := CSR_WADDRb
  REG.io.csr_wdataa := CSR_WDATAa
  REG.io.csr_wdatab := CSR_WDATAb

  REG.io.csr_raddr := CSR_RADDR

  CSR_RDATA := REG.io.csr_rdata

  // BCU Connections
  BCU.io.Branch <> GNU.io.out.Branch
  BCU.io.Zero   := EXU.io.out.Zero
  BCU.io.Less   := EXU.io.out.Less

  PCAsrc := BCU.io.PCAsrc
  PCBsrc := BCU.io.PCBsrc

  // Memory Connections
  io.mem_wraddr := EXU.io.out.Result
  io.mem_wdata := GPR_RDATAb
  io.mem_wop   := GNU.io.out.MemOp
  io.mem_wen   := GNU.io.out.MemWr

  io.pc_output := Cur_PC
}
