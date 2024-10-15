package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._

// riscv excution unit

class ysyx_23060198_EXU extends Module {
    val io = IO(new Bundle{
        // From CSR
        val IDU_2_EXU = Flipped(Decoupled(Input(new BUS_IDU_2_EXU)))
        val REG_2_EXU = Input(new BUS_REG_2_EXU)

        val EXU_2_WBU = Decoupled(Output(new BUS_EXU_2_WBU))
        val AXI = new AXI_Master
    })
    val alu = Module(new ysyx_23060198_ALU)
    val lsu = Module(new ysyx_23060198_LSU)

    when(io.IDU_2_EXU.bits.MemWr || io.IDU_2_EXU.bits.MemtoReg){
        alu.io.IDU_2_EXU.valid := false.B
        alu.io.out.ready := false.B
        io.IDU_2_EXU.ready <> lsu.io.IDU_2_EXU.ready
        io.IDU_2_EXU.valid <> lsu.io.IDU_2_EXU.valid
        io.EXU_2_WBU.ready <> lsu.io.out.ready
        io.EXU_2_WBU.valid <> lsu.io.out.valid
    }.otherwise{
        lsu.io.IDU_2_EXU.valid := false.B
        lsu.io.out.ready := false.B
        io.IDU_2_EXU.ready <> alu.io.IDU_2_EXU.ready
        io.IDU_2_EXU.valid <> alu.io.IDU_2_EXU.valid
        io.EXU_2_WBU.ready <> alu.io.out.ready
        io.EXU_2_WBU.valid <> alu.io.out.valid
    }

    val communication_succeed = (io.IDU_2_EXU.valid && io.IDU_2_EXU.ready)

    alu.io.IDU_2_EXU.bits := io.IDU_2_EXU.bits
    alu.io.CSR         := io.IDU_2_EXU.bits.CSR_rdata

    lsu.io.IDU_2_EXU.bits := io.IDU_2_EXU.bits
    lsu.io.AXI <> io.AXI
    
    io.EXU_2_WBU.bits.RegWr         := RegEnable(io.IDU_2_EXU.bits.RegWr, communication_succeed)
    io.EXU_2_WBU.bits.Branch        := RegEnable(io.IDU_2_EXU.bits.Branch, communication_succeed)
    io.EXU_2_WBU.bits.MemtoReg      := RegEnable(io.IDU_2_EXU.bits.MemtoReg, communication_succeed)
    io.EXU_2_WBU.bits.csr_ctr       := RegEnable(io.IDU_2_EXU.bits.csr_ctr, communication_succeed)
    io.EXU_2_WBU.bits.Imm           := RegEnable(io.IDU_2_EXU.bits.Imm, communication_succeed)
    io.EXU_2_WBU.bits.GPR_Adata     := RegEnable(io.IDU_2_EXU.bits.GPR_Adata, communication_succeed)
    io.EXU_2_WBU.bits.GPR_waddr     := RegEnable(io.IDU_2_EXU.bits.GPR_waddr, communication_succeed)
    io.EXU_2_WBU.bits.PC            := RegEnable(io.IDU_2_EXU.bits.PC, communication_succeed)
    io.EXU_2_WBU.bits.CSR_raddr     := RegEnable(io.IDU_2_EXU.bits.CSR_rdata, communication_succeed)
    io.EXU_2_WBU.bits.Result        := RegEnable(alu.io.out.bits.Result, communication_succeed)
    io.EXU_2_WBU.bits.Zero          := RegEnable(alu.io.out.bits.Zero, communication_succeed)
    io.EXU_2_WBU.bits.Less          := RegEnable(alu.io.out.bits.Less, communication_succeed)
    io.EXU_2_WBU.bits.Mem_rdata     := RegEnable(lsu.io.out.bits.Mem_rdata, communication_succeed)
}