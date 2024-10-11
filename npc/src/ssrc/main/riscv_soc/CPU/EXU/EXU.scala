package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._

// riscv excution unit

class ysyx_23060198_EXU extends Module {
    val io = IO(new Bundle{
        // From CSR
        val GNU_2_EXU = Flipped(Decoupled(Input(new BUS_GNU_2_EXU)))
        val REG_2_EXU = Input(new BUS_REG_2_EXU)

        val EXU_2_WBU = Decoupled(Output(new BUS_EXU_2_WBU))
        val AXI = new AXI_Master
    })
    val alu = Module(new ysyx_23060198_ALU)
    val lsu = Module(new ysyx_23060198_LSU)

    when(io.GNU_2_EXU.bits.MemWr || io.GNU_2_EXU.bits.MemtoReg){
        alu.io.GNU_2_EXU.valid := false.B
        alu.io.out.ready := false.B
        io.GNU_2_EXU.ready <> lsu.io.GNU_2_EXU.ready
        io.GNU_2_EXU.valid <> lsu.io.GNU_2_EXU.valid
        io.EXU_2_WBU.ready <> lsu.io.out.ready
        io.EXU_2_WBU.valid <> lsu.io.out.valid
    }.otherwise{
        lsu.io.GNU_2_EXU.valid := false.B
        lsu.io.out.ready := false.B
        io.GNU_2_EXU.ready <> alu.io.GNU_2_EXU.ready
        io.GNU_2_EXU.valid <> alu.io.GNU_2_EXU.valid
        io.EXU_2_WBU.ready <> alu.io.out.ready
        io.EXU_2_WBU.valid <> alu.io.out.valid
    }

    val comunication_succeed = (io.GNU_2_EXU.valid && io.GNU_2_EXU.ready)

    alu.io.GNU_2_EXU.bits := io.GNU_2_EXU.bits
    alu.io.CSR         := io.REG_2_EXU.CSR_rdata

    lsu.io.GNU_2_EXU.bits := io.GNU_2_EXU.bits
    lsu.io.AXI <> io.AXI

    io.EXU_2_WBU.bits.RegWr        <> RegEnable(io.GNU_2_EXU.bits.RegWr,        comunication_succeed)     
    io.EXU_2_WBU.bits.Branch       <> RegEnable(io.GNU_2_EXU.bits.Branch,       comunication_succeed)     
    io.EXU_2_WBU.bits.MemtoReg     <> RegEnable(io.GNU_2_EXU.bits.MemtoReg,     comunication_succeed) 
    io.EXU_2_WBU.bits.csr_ctr      <> RegEnable(io.GNU_2_EXU.bits.csr_ctr,      comunication_succeed)  
    io.EXU_2_WBU.bits.Imm          <> RegEnable(io.GNU_2_EXU.bits.Imm,          comunication_succeed)      
    io.EXU_2_WBU.bits.GPR_Adata    <> RegEnable(io.GNU_2_EXU.bits.GPR_Adata,    comunication_succeed)
    io.EXU_2_WBU.bits.GPR_waddr    <> RegEnable(io.GNU_2_EXU.bits.GPR_waddr,    comunication_succeed)
    io.EXU_2_WBU.bits.PC           <> RegEnable(io.GNU_2_EXU.bits.PC,           comunication_succeed)   
    io.EXU_2_WBU.bits.CSR          <> RegEnable(io.REG_2_EXU.CSR_rdata,         comunication_succeed)  
      
    io.EXU_2_WBU.bits.Result       <> alu.io.out.bits.Result
    io.EXU_2_WBU.bits.Zero         <> alu.io.out.bits.Zero
    io.EXU_2_WBU.bits.Less         <> alu.io.out.bits.Less
    io.EXU_2_WBU.bits.Mem_rdata    <> lsu.io.out.bits.Mem_rdata
}