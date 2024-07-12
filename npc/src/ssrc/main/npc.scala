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
  val LSU             = Module(new LSU)
  val WBU             = Module(new WBU)
  val REG             = Module(new REG()) 

  IFU.io.in.bits.inst <> io.Imem_rdata.bits
  IFU.io.in.ready     <> io.Imem_rdata.ready
  IFU.io.in.bits.addr <> REG.io.out.pc
  io.Imem_raddr       <> REG.io.out.pc
  IFU.io.in.valid     <> WBU.io.out.valid
  IFU.io.in.ready     <> WBU.io.out.ready
  
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
 
  // bus EXU -> LSU
  EXU.io.out.valid          <> LSU.io.in.valid
  EXU.io.out.ready          <> LSU.io.in.ready
  EXU.io.out.bits.RegWr     <> LSU.io.in.bits.RegWr    
  EXU.io.out.bits.Branch    <> LSU.io.in.bits.Branch   
  EXU.io.out.bits.MemtoReg  <> LSU.io.in.bits.MemtoReg 
  EXU.io.out.bits.MemWr     <> LSU.io.in.bits.MemWr    
  EXU.io.out.bits.MemOp     <> LSU.io.in.bits.MemOp    
  EXU.io.out.bits.csr_ctr   <> LSU.io.in.bits.csr_ctr  
  EXU.io.out.bits.Imm       <> LSU.io.in.bits.Imm      
  EXU.io.out.bits.GPR_Adata <> LSU.io.in.bits.GPR_Adata
  EXU.io.out.bits.GPR_Bdata <> LSU.io.in.bits.GPR_Bdata
  EXU.io.out.bits.GPR_waddr <> LSU.io.in.bits.GPR_waddr
  EXU.io.out.bits.PC        <> LSU.io.in.bits.PC       
  EXU.io.out.bits.CSR       <> LSU.io.in.bits.CSR      
  EXU.io.out.bits.Result    <> LSU.io.in.bits.Result   
  EXU.io.out.bits.Zero      <> LSU.io.in.bits.Zero     
  EXU.io.out.bits.Less      <> LSU.io.in.bits.Less     

  // bus LSU -> Dmem and Dmem -> LSU without delay
  io.Dmem_rdata                <> LSU.io.in.bits.Mem_rdata
  LSU.io.out.bits.Mem_wraddr   <> io.Dmem_wraddr
  LSU.io.out.bits.Mem_wdata    <> io.Dmem_wdata
  LSU.io.out.bits.MemOp        <> io.Dmem_wop
  LSU.io.out.bits.MemWr        <> io.Dmem_wen

  // bus LSU -> riscv_cpu
  LSU.io.out.valid          <> WBU.io.in.valid
  LSU.io.out.ready          <> WBU.io.in.ready
  LSU.io.out.bits.RegWr     <> WBU.io.in.bits.RegWr     
  LSU.io.out.bits.Branch    <> WBU.io.in.bits.Branch    
  LSU.io.out.bits.MemtoReg  <> WBU.io.in.bits.MemtoReg  
  LSU.io.out.bits.csr_ctr   <> WBU.io.in.bits.csr_ctr   
  LSU.io.out.bits.Imm       <> WBU.io.in.bits.Imm       
  LSU.io.out.bits.GPR_Adata <> WBU.io.in.bits.GPR_Adata 
  LSU.io.out.bits.GPR_waddr <> WBU.io.in.bits.GPR_waddr 
  LSU.io.out.bits.PC        <> WBU.io.in.bits.PC        
  LSU.io.out.bits.CSR       <> WBU.io.in.bits.CSR       
  LSU.io.out.bits.Result    <> WBU.io.in.bits.Result    
  LSU.io.out.bits.Zero      <> WBU.io.in.bits.Zero      
  LSU.io.out.bits.Less      <> WBU.io.in.bits.Less      
  LSU.io.out.bits.Mem_rdata <> WBU.io.in.bits.Mem_rdata 

  // bus riscv_cpu -> REG -> riscv_cpu with delay
  WBU.io.out.bits.inst_valid <> REG.io.in.inst_valid
  WBU.io.out.bits.Next_Pc    <> REG.io.in.pc
  WBU.io.out.bits.GPR_wdata  <> REG.io.in.GPR_wdata
  WBU.io.out.bits.GPR_waddr  <> REG.io.in.GPR_waddr
  WBU.io.out.bits.GPR_wen    <> REG.io.in.GPR_wen
  WBU.io.out.bits.CSR_ctr    <> REG.io.in.csr_ctr   
  WBU.io.out.bits.CSR_waddra <> REG.io.in.csr_waddra
  WBU.io.out.bits.CSR_waddrb <> REG.io.in.csr_waddrb
  WBU.io.out.bits.CSR_wdataa <> REG.io.in.csr_wdataa
  WBU.io.out.bits.CSR_wdatab <> REG.io.in.csr_wdatab

  val comp_cache = RegInit(Bool(), false.B)
  comp_cache := IFU.io.in.valid
  when((comp_cache === false.B) && (IFU.io.in.valid === true.B)) {
    io.inst_comp := true.B
  }.otherwise {
    io.inst_comp := false.B
  }
}
