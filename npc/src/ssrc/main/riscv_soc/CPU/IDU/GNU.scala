package riscv_cpu

import chisel3._
import chisel3.util._

// riscv generating number(all meassge ALU and other thing needs) unit

class GNU extends Module{
    val io = IO(new Bundle{
        val inst_input= Flipped(Decoupled(UInt(32.W)))
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
        val GPR_Adata= Output(UInt(32.W))
        val GPR_Bdata= Output(UInt(32.W))
        val PC       = Output(UInt(32.W))
    })

    io.inst_input.ready := true.B

    val inst = Wire(UInt(32.W))

    when(io.inst_input.valid) {
        inst := io.inst_input.bits
    }.otherwise {
        inst := NOP.U(32.W)
    }

    val idu = Module(new IDU)
    val igu = Module(new IGU)

    idu.io.inst <> inst
    idu.io.Regwr <> io.RegWr
    idu.io.Branch <> io.Branch
    idu.io.MemtoReg <> io.MemtoReg
    idu.io.MemWr <> io.MemWr
    idu.io.MemOp <> io.MemOp
    idu.io.ALUAsrc <> io.ALUAsrc
    idu.io.ALUBsrc <> io.ALUBsrc
    idu.io.ALUctr <> io.ALUctr
    idu.io.csr_ctr <> io.csr_ctr

    igu.io.inst <> inst
    igu.io.Extop <> idu.io.Extop
    igu.io.imm  <> io.Imm
}