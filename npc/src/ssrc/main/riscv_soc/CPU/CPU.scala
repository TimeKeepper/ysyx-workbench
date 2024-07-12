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

class CPU_LSU_input extends Bundle{
    val RegWr       = Input(Bool())
    val Branch      = Input(Bran_Type)
    val MemtoReg    = Input(Bool())
    val csr_ctr     = Input(CSR_Type)
    val Imm         = Input(UInt(32.W))
    val GPR_Adata   = Input(UInt(32.W))
    val GPR_waddr   = Input(UInt(5.W))
    val PC          = Input(UInt(32.W))
    val CSR         = Input(UInt(32.W))
    val Result      = Input(UInt(32.W))
    val Zero        = Input(Bool())
    val Less        = Input(Bool())
    val Mem_rdata   = Input(UInt(32.W))
}

class CPU() extends Module {
  val io = IO(new Bundle {
    val in     = Flipped(Decoupled(new CPU_LSU_input))
    val Imem_raddr     = Decoupled(UInt(32.W))

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

  io.Imem_raddr.valid := state === s_wait_ready
  io.in.ready := state === s_wait_valid

  // Modules
  val WBU             = Module(new WBU()) // Write Back Unit

  // 第一步 REG将pc输出给IFU读取指令 IFU将读取指令传递给GNU，
  io.Imem_raddr.bits <> io.reg_out.pc

  WBU.io.in.RegWr        <> io.in.bits.RegWr
  WBU.io.in.Branch       <> io.in.bits.Branch
  WBU.io.in.MemtoReg     <> io.in.bits.MemtoReg
  WBU.io.in.csr_ctr      <> io.in.bits.csr_ctr
  WBU.io.in.Imm          <> io.in.bits.Imm
  WBU.io.in.GPR_Adata    <> io.in.bits.GPR_Adata
  WBU.io.in.GPR_waddr    <> io.in.bits.GPR_waddr
  WBU.io.in.PC           <> io.in.bits.PC
  WBU.io.in.CSR          <> io.in.bits.CSR
  WBU.io.in.Result       <> io.in.bits.Result
  WBU.io.in.Zero         <> io.in.bits.Zero
  WBU.io.in.Less         <> io.in.bits.Less
  WBU.io.in.Mem_rdata    <> io.in.bits.Mem_rdata

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
}
