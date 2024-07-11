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
  val EXU             = Module(new EXU)
  val riscv_cpu       = Module(new CPU)
  val REG             = Module(new REG()) 

  IFU.io.in.bits.inst <> io.Imem_rdata.bits
  IFU.io.in.bits.addr <> riscv_cpu.io.Imem_raddr.bits
  IFU.io.in.valid     <> riscv_cpu.io.Imem_raddr.valid
  IFU.io.in.ready     <> riscv_cpu.io.Imem_raddr.ready
  IFU.io.in.ready     <> io.Imem_rdata.ready
  io.Imem_raddr       <> riscv_cpu.io.Imem_raddr.bits
  
  // bus IFU -> GNU
  IFU.io.out.valid      <> GNU.io.in.valid
  IFU.io.out.ready      <> GNU.io.in.ready
  IFU.io.out.bits.addr  <> GNU.io.in.bits.PC
  IFU.io.out.bits.inst  <> GNU.io.in.bits.inst

  // bus GNU -> REG -> GNU without delay
  IFU.io.out.bits.inst(19, 15) <> REG.io.in.GPR_raddra 
  IFU.io.out.bits.inst(24, 20) <> REG.io.in.GPR_raddrb 
  REG.io.out.GPR_rdataa <> GNU.io.in.bits.GPR_Adata
  REG.io.out.GPR_rdatab <> GNU.io.in.bits.GPR_Bdata

  // bus GNU -> EXU
  GNU.io.out.valid          <> EXU.io.in.valid        
  GNU.io.out.ready          <> EXU.io.in.ready     
  GNU.io.out.bits.RegWr     <> EXU.io.in.bits.RegWr    
  GNU.io.out.bits.Branch    <> EXU.io.in.bits.Branch   
  GNU.io.out.bits.MemtoReg  <> EXU.io.in.bits.MemtoReg 
  GNU.io.out.bits.MemWr     <> EXU.io.in.bits.MemWr    
  GNU.io.out.bits.MemOp     <> EXU.io.in.bits.MemOp    
  GNU.io.out.bits.ALUAsrc   <> EXU.io.in.bits.ALUAsrc  
  GNU.io.out.bits.ALUBsrc   <> EXU.io.in.bits.ALUBsrc  
  GNU.io.out.bits.ALUctr    <> EXU.io.in.bits.ALUctr   
  GNU.io.out.bits.csr_ctr   <> EXU.io.in.bits.csr_ctr  
  GNU.io.out.bits.Imm       <> EXU.io.in.bits.Imm      
  GNU.io.out.bits.GPR_Adata <> EXU.io.in.bits.GPR_Adata
  GNU.io.out.bits.GPR_Bdata <> EXU.io.in.bits.GPR_Bdata
  GNU.io.out.bits.GPR_waddr <> EXU.io.in.bits.GPR_waddr
  GNU.io.out.bits.PC        <> EXU.io.in.bits.PC     

  // bus GNU -> REG -> EXU without delay
  GNU.io.out.bits.CSR_raddr <> REG.io.in.csr_raddr  
  REG.io.out.csr_rdata      <> EXU.io.in.bits.CSR   
 
  // bus EXU -> riscv_cpu
  EXU.io.out.valid          <> riscv_cpu.io.in.valid
  EXU.io.out.ready          <> riscv_cpu.io.in.ready
  EXU.io.out.bits.RegWr     <> riscv_cpu.io.in.bits.RegWr    
  EXU.io.out.bits.Branch    <> riscv_cpu.io.in.bits.Branch   
  EXU.io.out.bits.MemtoReg  <> riscv_cpu.io.in.bits.MemtoReg 
  EXU.io.out.bits.MemWr     <> riscv_cpu.io.in.bits.MemWr    
  EXU.io.out.bits.MemOp     <> riscv_cpu.io.in.bits.MemOp    
  EXU.io.out.bits.csr_ctr   <> riscv_cpu.io.in.bits.csr_ctr  
  EXU.io.out.bits.Imm       <> riscv_cpu.io.in.bits.Imm      
  EXU.io.out.bits.GPR_Adata <> riscv_cpu.io.in.bits.GPR_Adata
  EXU.io.out.bits.GPR_Bdata <> riscv_cpu.io.in.bits.GPR_Bdata
  EXU.io.out.bits.GPR_waddr <> riscv_cpu.io.in.bits.GPR_waddr
  EXU.io.out.bits.PC        <> riscv_cpu.io.in.bits.PC       
  EXU.io.out.bits.CSR       <> riscv_cpu.io.in.bits.CSR      
  EXU.io.out.bits.Result    <> riscv_cpu.io.in.bits.Result   
  EXU.io.out.bits.Zero      <> riscv_cpu.io.in.bits.Zero     
  EXU.io.out.bits.Less      <> riscv_cpu.io.in.bits.Less     

  // bus riscv_cpu -> Dmem and Dmem -> riscv_cpu without delay
  io.Dmem_rdata             <> riscv_cpu.io.Dmem_rdata
  riscv_cpu.io.Dmem_wraddr  <> io.Dmem_wraddr
  riscv_cpu.io.Dmem_wdata   <> io.Dmem_wdata
  riscv_cpu.io.Dmem_wop     <> io.Dmem_wop
  riscv_cpu.io.Dmem_wen     <> io.Dmem_wen

  // bus riscv_cpu -> REG -> riscv_cpu with delay
  riscv_cpu.io.reg_in.inst_valid <> REG.io.in.inst_valid
  riscv_cpu.io.reg_in.GPR_wdata  <> REG.io.in.GPR_wdata
  riscv_cpu.io.reg_in.GPR_waddr  <> REG.io.in.GPR_waddr
  riscv_cpu.io.reg_in.GPR_wen    <> REG.io.in.GPR_wen
  riscv_cpu.io.reg_in.pc         <> REG.io.in.pc
  riscv_cpu.io.reg_in.csr_ctr    <> REG.io.in.csr_ctr   
  riscv_cpu.io.reg_in.csr_waddra <> REG.io.in.csr_waddra
  riscv_cpu.io.reg_in.csr_waddrb <> REG.io.in.csr_waddrb
  riscv_cpu.io.reg_in.csr_wdataa <> REG.io.in.csr_wdataa
  riscv_cpu.io.reg_in.csr_wdatab <> REG.io.in.csr_wdatab
  riscv_cpu.io.reg_out.pc        <> REG.io.out.pc

  val comp_cache = RegInit(Bool(), false.B)
  comp_cache := IFU.io.in.valid
  when((comp_cache === false.B) && (IFU.io.in.valid === true.B)) {
    io.inst_comp := true.B
  }.otherwise {
    io.inst_comp := false.B
  }
}
