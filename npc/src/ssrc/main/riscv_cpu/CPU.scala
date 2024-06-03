package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

class CPU() extends Module {
  val io = IO(new Bundle {
    val inst      = Flipped(Decoupled(UInt(32.W)))
    val mem_rdata = Input(UInt(32.W))
    val mem_raddr = Output(UInt(32.W))

    val mem_wdata = Output(UInt(32.W))
    val mem_wop   = Output(MemOp_Type)
    val mem_wen   = Output(Bool())
  })

  io.inst.ready := true.B

  // Modules
  val IDU = Module(new IDU()) // Instruction Decode Unit
  val IGU = Module(new IGU()) // Immediate Generation Unit
  val REG = Module(new REG()) // Register File
  val ALU = Module(new ALU()) // Arithmetic and Logic Unit
  val BCU = Module(new BCU()) // Branch Control Unit

  // wires
  val ExtOp    = Wire(ExtOp_Type)
  val RegWr    = Wire(Bool())
  val MemtoReg = Wire(Bool())
  val MemWr    = Wire(Bool())
  val MemOp    = Wire(MemOp_Type)
  val ALUAsrc  = Wire(ALUAsrc_Type)
  val ALUBsrc  = Wire(ALUBSrc_Type)
  val ALUctr   = Wire(ALUctr_Type)
  val csr_ctr  = Wire(CSR_Type)

  val Imm = Wire(UInt(32.W))

  val GPR_WADDR  = Wire(UInt(5.W))
  val GPR_WDATA  = Wire(UInt(32.W))
  val GPR_RADDRa = Wire(UInt(5.W))
  val GPR_RADDRb = Wire(UInt(5.W))
  val GPR_RDATAa = Wire(UInt(32.W))
  val GPR_RDATAb = Wire(UInt(32.W))

  val Next_PC = Wire(UInt(32.W))
  val Cur_PC  = Wire(UInt(32.W))

  val CSR_WADDRa = Wire(UInt(12.W))
  val CSR_WADDRb = Wire(UInt(12.W))
  val CSR_WDATAa = Wire(UInt(32.W))
  val CSR_WDATAb = Wire(UInt(32.W))
  val CSR_RADDR  = Wire(UInt(12.W))
  val CSR_RDATA  = Wire(UInt(32.W))

  val Less   = Wire(Bool())
  val Zero   = Wire(Bool())
  val Result = Wire(UInt(32.W))

  val PCAsrc = Wire(PCAsrc_Type)
  val PCBsrc = Wire(PCBsrc_Type)

  // IDU Connections
  IDU.io.inst := io.inst.bits

  ExtOp    := IDU.io.ExtOp
  RegWr    := IDU.io.RegWr
  MemtoReg := IDU.io.MemtoReg
  MemWr    := IDU.io.MemWr
  MemOp    := IDU.io.MemOp
  ALUAsrc  := IDU.io.ALUAsrc
  ALUBsrc  := IDU.io.ALUBsrc
  ALUctr   := IDU.io.ALUctr
  csr_ctr  := IDU.io.csr_ctr

  // IGU Connections
  IGU.io.inst  := io.inst.bits
  IGU.io.Extop := ExtOp

  Imm := IGU.io.imm

  // REG Connections
  GPR_WADDR := io.inst.bits(11, 7)

  when(MemtoReg) {
    GPR_WDATA := io.mem_rdata
  }.otherwise {
    GPR_WDATA := Result
  }

  GPR_WADDR := io.inst.bits(11, 7)

  REG.io.wdata := GPR_WDATA
  REG.io.waddr := GPR_WADDR
  REG.io.wen   := RegWr

  GPR_RADDRa    := io.inst.bits(19, 15)
  GPR_RADDRb    := io.inst.bits(24, 20)
  REG.io.raddra := GPR_RADDRa
  REG.io.raddrb := GPR_RADDRb
  GPR_RDATAa    := REG.io.rdataa
  GPR_RDATAb    := REG.io.rdatab

  val PCAval = Wire(UInt(32.W))
  val PCBval = Wire(UInt(32.W))

  when(PCAsrc === PCAsrc_Imm) {
    PCAval := Imm
  }.elsewhen(PCAsrc === PCAsrc_4) {
    PCAval := 4.U
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

  when(csr_ctr === CSR_R1W0) {
    CSR_RADDR := "h341".U // instruction mret read mepc to recovered pc
  }.elsewhen(csr_ctr === CSR_R1W2) {
    CSR_RADDR := "h305".U // instruction ecall read mtevc to get to error order function
  }.otherwise {
    CSR_RADDR := Imm(11, 0)
  }

  when(csr_ctr === CSR_R1W2) {
    CSR_WADDRa := "h341".U // instruction ecall use csr mepc
  }.otherwise {
    CSR_WADDRa := Imm(11, 0)
  }

  when(csr_ctr === CSR_R1W2) {
    CSR_WDATAa := Cur_PC // instruction ecall store current pc
  }.otherwise {
    CSR_WDATAa := GPR_RDATAa
  }

  CSR_WADDRb := "h342".U // instruction ecall write mstatus
  CSR_WDATAb := 11.U // for now, only set error status 11

  REG.io.csr_ctr    := csr_ctr
  REG.io.csr_waddra := CSR_WADDRa
  REG.io.csr_waddrb := CSR_WADDRb
  REG.io.csr_wdataa := CSR_WDATAa
  REG.io.csr_wdatab := CSR_WDATAb

  REG.io.csr_raddr := CSR_RADDR

  CSR_RDATA := REG.io.csr_rdata

  // ALU Connections
  ALU.io.ALUctr := ALUctr

  when(ALUAsrc === A_RS1) {
    ALU.io.src_A := GPR_RDATAa
  }.elsewhen(ALUAsrc === A_PC) {
    ALU.io.src_A := Cur_PC
  }.elsewhen(ALUAsrc === A_CSR) {
    ALU.io.src_A := CSR_RDATA
  }.otherwise {
    ALU.io.src_A := 0.U
  }

  when(ALUBsrc === B_RS2) {
    ALU.io.src_B := GPR_RDATAb
  }.elsewhen(ALUBsrc === B_IMM) {
    ALU.io.src_B := Imm
  }.elsewhen(ALUBsrc === B_4) {
    ALU.io.src_B := 4.U
  }.otherwise {
    ALU.io.src_B := GPR_RDATAa
  }

  Result := ALU.io.ALUout
  Zero   := ALU.io.Zero
  Less   := ALU.io.Less

  // BCU Connections
  BCU.io.Branch <> IDU.io.Branch
  BCU.io.Zero   := Zero
  BCU.io.Less   := Less

  PCAsrc := BCU.io.PCAsrc
  PCBsrc := BCU.io.PCBsrc

  // Memory Connections
  io.mem_raddr := Result
  io.mem_wdata := GPR_RDATAb
  io.mem_wop   := MemOp
  io.mem_wen   := MemWr
}
