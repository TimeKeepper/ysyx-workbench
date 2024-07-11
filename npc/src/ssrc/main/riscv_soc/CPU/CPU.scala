package riscv_cpu

import chisel3._
import chisel3.util._

import Instructions._
import signal_value._

class CPU_REG_input extends Bundle{
  val inst_valid = Output(Bool())

  val GPR_wdata = Output(UInt(32.W))
  val GPR_waddr = Output(UInt(5.W))
  val GPR_wen   = Output(Bool())

  val pc  = Output(UInt(32.W))

  val csr_ctr    = Output(CSR_Type)
  val csr_waddra = Output(UInt(12.W))
  val csr_waddrb = Output(UInt(12.W))
  val csr_wdataa = Output(UInt(32.W))
  val csr_wdatab = Output(UInt(32.W))
}

class CPU_REG_output extends Bundle{
  val pc = Input(UInt(32.W))

  val csr_rdata = Input(UInt(32.W))
}

class CPU_GNU_output extends Bundle{
    val RegWr    = Input(Bool())
    val Branch   = Input(Bran_Type)
    val MemtoReg = Input(Bool())
    val MemWr    = Input(Bool())
    val MemOp    = Input(MemOp_Type)
    val ALUAsrc  = Input(ALUAsrc_Type)
    val ALUBsrc  = Input(ALUBSrc_Type)
    val ALUctr   = Input(ALUctr_Type)
    val csr_ctr  = Input(CSR_Type)
    val Imm      = Input(UInt(32.W))
    val GPR_Adata = Input(UInt(32.W))
    val GPR_Bdata = Input(UInt(32.W))
    val GPR_waddr = Input(UInt(5.W))
    val PC       = Input(UInt(32.W))
}

class CPU() extends Module {
  val io = IO(new Bundle {
    val in     = Flipped(Decoupled(new CPU_GNU_output))
    val Imem_raddr     = Decoupled(UInt(32.W))
    val Dmem_rdata     = Input(UInt(32.W))

    val Dmem_wdata     = Output(UInt(32.W))
    val Dmem_wop       = Output(MemOp_Type)
    val Dmem_wen       = Output(Bool())
    
    val Dmem_wraddr    = Output(UInt(32.W))

    val reg_in = new CPU_REG_input
    val reg_out = new CPU_REG_output
  })

  val s_wait_valid :: s_wait_ready :: s_busy :: Nil = Enum(3)
  val state = RegInit(s_wait_ready)

  state := MuxLookup(state, s_wait_valid)(
    Seq(
        s_wait_valid -> Mux(io.in.valid, s_wait_ready, s_wait_valid),
        s_wait_ready -> Mux(io.Imem_raddr.ready, s_wait_valid, s_wait_ready),
    )
  )

  // Modules
  val EXU             = Module(new EXU()) // Execution Unit
  val WBU             = Module(new WBU()) // Write Back Unit

  io.Imem_raddr.valid := state === s_wait_ready
  io.in.ready := state === s_wait_valid

  // 第一步 REG将pc输出给IFU读取指令 IFU将读取指令传递给GNU，
  io.Imem_raddr.bits <> io.reg_out.pc

  EXU.io.in.RegWr        <> io.in.bits.RegWr
  EXU.io.in.Branch       <> io.in.bits.Branch
  EXU.io.in.MemtoReg     <> io.in.bits.MemtoReg
  EXU.io.in.MemWr        <> io.in.bits.MemWr
  EXU.io.in.MemOp        <> io.in.bits.MemOp
  EXU.io.in.ALUAsrc      <> io.in.bits.ALUAsrc
  EXU.io.in.ALUBsrc      <> io.in.bits.ALUBsrc
  EXU.io.in.ALUctr       <> io.in.bits.ALUctr
  EXU.io.in.csr_ctr      <> io.in.bits.csr_ctr
  EXU.io.in.Imm          <> io.in.bits.Imm
  EXU.io.in.GPR_Adata    <> io.in.bits.GPR_Adata
  EXU.io.in.GPR_Bdata    <> io.in.bits.GPR_Bdata
  EXU.io.in.GPR_waddr    <> io.in.bits.GPR_waddr
  EXU.io.in.PC           <> io.in.bits.PC
  EXU.io.in.CSR          <> io.reg_out.csr_rdata

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
  WBU.io.in.CSR          <> io.reg_out.csr_rdata
  WBU.io.in.Result       <> EXU.io.out.Result
  WBU.io.in.Zero         <> EXU.io.out.Zero
  WBU.io.in.Less         <> EXU.io.out.Less

  WBU.io.in.Mem_rdata    <> io.Dmem_rdata

  io.reg_in.inst_valid <> io.in.valid
  io.reg_in.GPR_wdata <> WBU.io.out.GPR_wdata
  io.reg_in.GPR_waddr <> WBU.io.out.GPR_waddr
  io.reg_in.GPR_wen   <> WBU.io.out.GPR_wen
  io.reg_in.pc        <> WBU.io.out.Next_Pc

  io.reg_in.csr_ctr    := WBU.io.out.CSR_ctr
  io.reg_in.csr_waddra := WBU.io.out.CSR_waddra
  io.reg_in.csr_waddrb := WBU.io.out.CSR_waddrb
  io.reg_in.csr_wdataa := WBU.io.out.CSR_wdataa
  io.reg_in.csr_wdatab := WBU.io.out.CSR_wdatab

  io.Dmem_wraddr := EXU.io.out.Result
  io.Dmem_wdata := EXU.io.out.GPR_Bdata
  io.Dmem_wop   := io.in.bits.MemOp
  io.Dmem_wen   := io.in.bits.MemWr & (state === s_wait_ready)
}
