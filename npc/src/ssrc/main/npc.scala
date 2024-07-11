package npc

import riscv_cpu._
import ram._

import chisel3._
import chisel3.util._

class npc extends Module {
  val io = IO(new Bundle {
    val Imem_rdata = Flipped(Decoupled(UInt(32.W)))
    val Imem_raddr = Output(UInt(32.W))
    val Dmem_rdata = Input(UInt(32.W))
    val Dmem_wraddr = Output(UInt(32.W))

    val Dmem_wdata = Output(UInt(32.W))
    val Dmem_wop   = Output(UInt(3.W))
    val Dmem_wen   = Output(Bool())

    val inst_comp  = Output(Bool())
  })
  
  val IFU             = Module(new IFU)
  val GNU             = Module(new GNU)
  val riscv_cpu       = Module(new CPU)
  val REG             = Module(new REG()) 

  IFU.io.in.bits.inst <> io.Imem_rdata.bits
  IFU.io.in.valid     <> riscv_cpu.io.Imem_raddr.valid
  IFU.io.in.ready     <> riscv_cpu.io.Imem_raddr.ready
  IFU.io.in.ready     <> io.Imem_rdata.ready
  IFU.io.in.bits.addr <> riscv_cpu.io.Imem_raddr.bits
  io.Imem_raddr  <> riscv_cpu.io.Imem_raddr.bits
  
  IFU.io.out.valid      <> GNU.io.in.valid
  IFU.io.out.ready      <> GNU.io.in.ready
  IFU.io.out.bits.addr  <> GNU.io.in.bits.PC
  IFU.io.out.bits.inst  <> GNU.io.in.bits.inst
  REG.io.in.GPR_raddra  <> IFU.io.out.bits.inst(19, 15)
  REG.io.in.GPR_raddrb  <> IFU.io.out.bits.inst(24, 20)
  REG.io.out.GPR_rdataa <> GNU.io.in.bits.GPR_Adata
  REG.io.out.GPR_rdatab <> GNU.io.in.bits.GPR_Bdata
  REG.io.in.csr_raddr   <> GNU.io.out.bits.CSR_raddr

  GNU.io.out.valid         <> riscv_cpu.io.in.valid        
  GNU.io.out.ready         <> riscv_cpu.io.in.ready        
  GNU.io.out.bits.inst      <> riscv_cpu.io.in.bits.inst     
  GNU.io.out.bits.RegWr     <> riscv_cpu.io.in.bits.RegWr    
  GNU.io.out.bits.Branch    <> riscv_cpu.io.in.bits.Branch   
  GNU.io.out.bits.MemtoReg  <> riscv_cpu.io.in.bits.MemtoReg 
  GNU.io.out.bits.MemWr     <> riscv_cpu.io.in.bits.MemWr    
  GNU.io.out.bits.MemOp     <> riscv_cpu.io.in.bits.MemOp    
  GNU.io.out.bits.ALUAsrc   <> riscv_cpu.io.in.bits.ALUAsrc  
  GNU.io.out.bits.ALUBsrc   <> riscv_cpu.io.in.bits.ALUBsrc  
  GNU.io.out.bits.ALUctr    <> riscv_cpu.io.in.bits.ALUctr   
  GNU.io.out.bits.csr_ctr   <> riscv_cpu.io.in.bits.csr_ctr  
  GNU.io.out.bits.Imm       <> riscv_cpu.io.in.bits.Imm      
  GNU.io.out.bits.GPR_Adata <> riscv_cpu.io.in.bits.GPR_Adata
  GNU.io.out.bits.GPR_Bdata <> riscv_cpu.io.in.bits.GPR_Bdata
  GNU.io.out.bits.GPR_waddr <> riscv_cpu.io.in.bits.GPR_waddr
  GNU.io.out.bits.PC        <> riscv_cpu.io.in.bits.PC       

  riscv_cpu.io.Dmem_rdata  <> io.Dmem_rdata
  riscv_cpu.io.Dmem_wraddr <> io.Dmem_wraddr

  riscv_cpu.io.Dmem_wdata  <> io.Dmem_wdata
  riscv_cpu.io.Dmem_wop    <> io.Dmem_wop
  riscv_cpu.io.Dmem_wen    <> io.Dmem_wen

  riscv_cpu.io.reg_in.inst_valid <> REG.io.in.inst_valid
  riscv_cpu.io.reg_in.GPR_wdata <> REG.io.in.GPR_wdata
  riscv_cpu.io.reg_in.GPR_waddr <> REG.io.in.GPR_waddr
  riscv_cpu.io.reg_in.GPR_wen   <> REG.io.in.GPR_wen
  riscv_cpu.io.reg_in.pc         <> REG.io.in.pc
  riscv_cpu.io.reg_in.csr_ctr    <> REG.io.in.csr_ctr   
  riscv_cpu.io.reg_in.csr_waddra <> REG.io.in.csr_waddra
  riscv_cpu.io.reg_in.csr_waddrb <> REG.io.in.csr_waddrb
  riscv_cpu.io.reg_in.csr_wdataa <> REG.io.in.csr_wdataa
  riscv_cpu.io.reg_in.csr_wdatab <> REG.io.in.csr_wdatab

  riscv_cpu.io.reg_out.pc         <> REG.io.out.pc
  riscv_cpu.io.reg_out.csr_rdata  <> REG.io.out.csr_rdata

  val comp_cache = RegInit(Bool(), false.B)
  comp_cache := IFU.io.in.valid
  when((comp_cache === false.B) && (IFU.io.in.valid === true.B)) {
    io.inst_comp := true.B
  }.otherwise {
    io.inst_comp := false.B
  }
}
