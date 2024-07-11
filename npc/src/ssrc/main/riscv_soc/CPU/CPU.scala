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
}

class CPU_EXU_input extends Bundle{
    val RegWr       = Output(Bool())
    val Branch      = Output(Bran_Type)
    val MemtoReg    = Output(Bool())
    val MemWr       = Output(Bool())
    val MemOp       = Output(MemOp_Type)
    val csr_ctr     = Output(CSR_Type)
    val Imm         = Output(UInt(32.W))
    val GPR_Adata   = Output(UInt(32.W))
    val GPR_Bdata   = Output(UInt(32.W))
    val GPR_waddr   = Output(UInt(5.W))
    val PC          = Output(UInt(32.W))
    val CSR         = Output(UInt(32.W))
    val Result      = Output(UInt(32.W))
    val Zero        = Output(Bool())
    val Less        = Output(Bool())
}

class CPU() extends Module {
  val io = IO(new Bundle {
    val in     = Flipped(Decoupled(new CPU_EXU_input))
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
  val WBU             = Module(new WBU()) // Write Back Unit

  io.Imem_raddr.valid := state === s_wait_ready
  io.in.ready := state === s_wait_valid

  // 第一步 REG将pc输出给IFU读取指令 IFU将读取指令传递给GNU，
  io.Imem_raddr.bits <> io.reg_out.pc

  // 第二步，EXU处理完成之后将结果传递给WBU，WBU根据结果更新系统状态，包括GPR，CSR，PC以及内存
  WBU.io.in.RegWr        <> io.in.bits.RegWr
  WBU.io.in.Branch       <> io.in.bits.Branch
  WBU.io.in.MemtoReg     <> io.in.bits.MemtoReg
  WBU.io.in.MemWr        <> io.in.bits.MemWr
  WBU.io.in.MemOp        <> io.in.bits.MemOp
  WBU.io.in.csr_ctr      <> io.in.bits.csr_ctr
  WBU.io.in.Imm          <> io.in.bits.Imm
  WBU.io.in.GPR_Adata    <> io.in.bits.GPR_Adata
  WBU.io.in.GPR_Bdata    <> io.in.bits.GPR_Bdata
  WBU.io.in.GPR_waddr    <> io.in.bits.GPR_waddr
  WBU.io.in.PC           <> io.in.bits.PC
  WBU.io.in.Result       <> io.in.bits.Result
  WBU.io.in.Zero         <> io.in.bits.Zero
  WBU.io.in.Less         <> io.in.bits.Less
  WBU.io.in.CSR          <> io.in.bits.CSR

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

  io.Dmem_wraddr := io.in.bits.Result
  io.Dmem_wdata := io.in.bits.GPR_Bdata
  io.Dmem_wop   := io.in.bits.MemOp
  io.Dmem_wen   := io.in.bits.MemWr & (state === s_wait_ready)
}
