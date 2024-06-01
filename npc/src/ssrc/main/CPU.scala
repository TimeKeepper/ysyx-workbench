package riscv_cpu

import chisel3._
import chisel3.util._

class CPU() extends Module {
    val io = IO(new Bundle {
        val inst = Input(UInt(32.W))
        val mem_rdata = Input(UInt(32.W))
        val mem_raddr = Output(UInt(32.W))

        val mem_wdata = Output(UInt(32.W))
        val mem_wop = Output(UInt(3.W))
        val mem_wen = Output(Bool())
    })

    // Modules
    val IDU = Module(new IDU()) // Instruction Decode Unit
    val IGU = Module(new IGU()) // Immediate Generation Unit
    val REG = Module(new REG()) // Register File
    val ALU = Module(new ALU()) // Arithmetic and Logic Unit
    val BCU = Module(new BCU()) // Branch Control Unit

    // wires
    val ExtOp = Wire(UInt(3.W))
    val RegWr = Wire(Bool())
    val Branch = Wire(UInt(3.W))
    val MemtoReg = Wire(Bool())
    val MemWr  = Wire(Bool())
    val MemOp  = Wire(UInt(3.W))
    val ALUAsrc = Wire(UInt(2.W))
    val ALUBsrc = Wire(UInt(2.W))
    val ALUctr = Wire(UInt(4.W))
    val csr_ctr = Wire(UInt(2.W))
    
    val Imm = Wire(UInt(32.W))

    val GPR_WADDR = Wire(UInt(5.W))
    val GPR_WDATA = Wire(UInt(32.W))
    val GPR_RADDRa = Wire(UInt(5.W))
    val GPR_RADDRb = Wire(UInt(5.W))
    val GPR_RDATAa = Wire(UInt(32.W))
    val GPR_RDATAb = Wire(UInt(32.W))

    val Next_PC = Wire(UInt(32.W))
    val Cur_PC = Wire(UInt(32.W))

    val CSR_WADDRa = Wire(UInt(12.W))
    val CSR_WADDRb = Wire(UInt(12.W))
    val CSR_WDATAa = Wire(UInt(32.W))
    val CSR_WDATAb = Wire(UInt(32.W))
    val CSR_RADDR = Wire(UInt(12.W))
    val CSR_RDATA = Wire(UInt(32.W))

    val Less = Wire(Bool())
    val Zero = Wire(Bool())
    val Result = Wire(UInt(32.W))

    val PCAsrc = Wire(Bool())
    val PCBsrc = Wire(Bool())
    
    // IDU Connections
    IDU.io.inst := io.inst

    ExtOp := IDU.io.ExtOp
    RegWr := IDU.io.RegWr
    Branch := IDU.io.Branch
    MemtoReg := IDU.io.MemtoReg
    MemWr := IDU.io.MemWr
    MemOp := IDU.io.MemOp
    ALUAsrc := IDU.io.ALUAsrc
    ALUBsrc := IDU.io.ALUBsrc
    ALUctr := IDU.io.ALUctr
    csr_ctr := IDU.io.csr_ctr

    // IGU Connections
    IGU.io.inst := io.inst
    IGU.io.Extop := ExtOp

    Imm := IGU.io.imm

    // REG Connections
    GPR_WADDR := io.inst(11, 7)

    when(MemtoReg) {
        GPR_WDATA := io.mem_rdata
    }.otherwise {
        GPR_WDATA := Result
    }

    GPR_WADDR := io.inst(11, 7)

    REG.io.wdata := GPR_WDATA
    REG.io.waddr := GPR_WADDR
    REG.io.wen := RegWr

    GPR_RADDRa := io.inst(19, 15)
    GPR_RADDRb := io.inst(24, 20)
    REG.io.raddra := GPR_RADDRa
    REG.io.raddrb := GPR_RADDRb
    GPR_RDATAa := REG.io.rdataa
    GPR_RDATAb := REG.io.rdatab

    val PCAval = Wire(UInt(32.W))
    val PCBval = Wire(UInt(32.W))

    when(PCAsrc) {
        PCAval := Imm
    }.otherwise {
        PCAval := 4.U
    }

    when(PCBsrc) {
        PCBval := GPR_RDATAa
    }.otherwise {
        PCBval := Cur_PC
    }

    when(csr_ctr(0)) {
        Next_PC := CSR_RDATA
    }.otherwise {
        Next_PC := PCAval + PCBval
    }

    REG.io.pc_in := Next_PC
    Cur_PC := REG.io.pc_out

    when(csr_ctr === 3.U) {
        CSR_WADDRa := "h341".U
    }.otherwise {
        CSR_WADDRa := Imm(11, 0)
    }

    when(csr_ctr === 3.U) {
        CSR_WDATAa := Cur_PC
    }.otherwise {
        CSR_WDATAa := Imm(11, 0)
    }

    CSR_WADDRb := "h342".U
    CSR_WDATAb := 11.U

    REG.io.csr_ctr := csr_ctr
    REG.io.csr_waddra := CSR_WADDRa
    REG.io.csr_waddrb := CSR_WADDRb
    REG.io.csr_wdataa := CSR_WDATAa
    REG.io.csr_wdatab := CSR_WDATAb

    REG.io.csr_raddr := CSR_RADDR
    CSR_RDATA := REG.io.csr_rdata

    // ALU Connections
    ALU.io.ALUctr := ALUctr

    when(ALUAsrc === 0.U) {
        ALU.io.src_A := GPR_RDATAa
    }.elsewhen (ALUAsrc === 1.U) {
        ALU.io.src_A := Next_PC
    }.elsewhen (ALUAsrc === 2.U) {
        ALU.io.src_A := CSR_RDATA
    }.otherwise {
        ALU.io.src_A := 0.U
    }

    when(ALUBsrc === 0.U) {
        ALU.io.src_B := GPR_RDATAb
    }.elsewhen(ALUBsrc === 1.U) {
        ALU.io.src_B := Imm
    }.elsewhen(ALUBsrc === 2.U) {
        ALU.io.src_B := 4.U
    }.otherwise {
        ALU.io.src_B := GPR_RDATAa
    }

    Result := ALU.io.ALUout
    Zero := ALU.io.Zero
    Less := ALU.io.Less

    // BCU Connections
    BCU.io.Branch := Branch
    BCU.io.Zero := Zero
    BCU.io.Less := Less

    PCAsrc := BCU.io.PCAsrc
    PCBsrc := BCU.io.PCBsrc

    // Memory Connections
    io.mem_raddr := Result
    io.mem_wdata := GPR_RDATAb
    io.mem_wop := MemOp
    io.mem_wen := MemWr
}