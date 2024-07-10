package riscv_cpu

import chisel3._
import chisel3.util._

import Instructions._
import signal_value._

class CPU() extends Module {
  val io = IO(new Bundle {
    val Imem_rdata     = Flipped(Decoupled(UInt(32.W)))
    val Imem_raddr     = Decoupled(UInt(32.W))
    val Dmem_rdata     = Input(UInt(32.W))

    val Dmem_wdata     = Output(UInt(32.W))
    val Dmem_wop       = Output(MemOp_Type)
    val Dmem_wen       = Output(Bool())
    
    val Dmem_wraddr    = Output(UInt(32.W))
  })

  val s_wait_valid :: s_wait_ready :: s_busy :: Nil = Enum(3)
  val state = RegInit(s_wait_valid)

  state := MuxLookup(state, s_wait_valid)(
    Seq(
        s_wait_valid -> Mux(io.Imem_rdata.valid, s_wait_ready, s_wait_valid),
        s_wait_ready -> Mux(io.Imem_raddr.ready, s_wait_valid, s_wait_ready),
    )
  )

  // Modules
  val GNU             = Module(new GNU()) // Generating Number Unit
  val EXU             = Module(new EXU()) // Execution Unit
  val WBU             = Module(new WBU()) // Write Back Unit
  val REG             = Module(new REG()) // Register File

  io.Imem_raddr.valid := state === s_wait_ready
  io.Imem_rdata.ready := state === s_wait_valid

  val Imem_raddr_cache = RegInit("h80000000".U(32.W))
  when(io.Imem_raddr.valid && io.Imem_raddr.ready){
    Imem_raddr_cache := REG.io.pc_out
  } 

  // 第一步 REG将pc输出给IFU读取指令 IFU将读取指令传递给GNU，
  io.Imem_raddr.bits <> Imem_raddr_cache

  GNU.io.in.inst <> Mux(io.Imem_rdata.valid, io.Imem_rdata.bits, NOP.U)
  GNU.io.in.PC   <> REG.io.pc_out

  // GNU处理完成之后传递给REG读取两个GPR德值并返回给GNU，
  GNU.io.out.inst(19, 15) <> REG.io.GPR_raddra
  GNU.io.out.inst(24, 20) <> REG.io.GPR_raddrb
  GNU.io.in.GPR_Adata <> REG.io.GPR_rdataa
  GNU.io.in.GPR_Bdata <> REG.io.GPR_rdatab

  // GNU将控制信号和两个寄存器值传递给EXU，同时根据需要读取的地址将csr寄存器的值传递给EXU
  GNU.io.out.CSR_raddr <> REG.io.csr_raddr

  EXU.io.in.RegWr        <> GNU.io.out.RegWr
  EXU.io.in.Branch       <> Mux(io.Imem_rdata.valid, GNU.io.out.Branch, Bran_NoC)
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
  EXU.io.in.CSR          <> REG.io.csr_rdata

  // 第二步，EXU处理完成之后将结果传递给WBU，WBU根据结果更新系统状态，包括GPR，CSR，PC以及内存
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
  WBU.io.in.CSR          <> REG.io.csr_rdata
  WBU.io.in.Result       <> EXU.io.out.Result
  WBU.io.in.Zero         <> EXU.io.out.Zero
  WBU.io.in.Less         <> EXU.io.out.Less

  WBU.io.in.Mem_rdata    <> io.Dmem_rdata

  REG.io.GPR_wdata <> WBU.io.out.GPR_wdata
  REG.io.GPR_waddr <> WBU.io.out.GPR_waddr
  REG.io.GPR_wen   <> WBU.io.out.GPR_wen
  REG.io.pc_in     <> WBU.io.out.Next_Pc

  REG.io.csr_ctr    := WBU.io.out.CSR_ctr
  REG.io.csr_waddra := WBU.io.out.CSR_waddra
  REG.io.csr_waddrb := WBU.io.out.CSR_waddrb
  REG.io.csr_wdataa := WBU.io.out.CSR_wdataa
  REG.io.csr_wdatab := WBU.io.out.CSR_wdatab

  io.Dmem_wraddr := EXU.io.out.Result
  io.Dmem_wdata := EXU.io.out.GPR_Bdata
  io.Dmem_wop   := GNU.io.out.MemOp
  io.Dmem_wen   := GNU.io.out.MemWr
}
