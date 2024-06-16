package riscv_cpu

import chisel3._
import chisel3.util._

import Instructions._
import signal_value._

class CPU() extends Module {
  val io = IO(new Bundle {
    val inst_input = Flipped(Decoupled(UInt(32.W)))
    val pc_output  = Output(UInt(32.W))
    val mem_rdata  = Input(UInt(32.W))

    val mem_wdata = Output(UInt(32.W))
    val mem_wop   = Output(MemOp_Type)
    val mem_wen   = Output(Bool())

    val mem_wraddr = Output(UInt(32.W))
  })

  // Modules
  val IFU = Module(new IFU()) // Instruction Fetch Unit
  val GNU = Module(new GNU()) // Generating Number Unit
  val EXU = Module(new EXU()) // Execution Unit
  val WBU = Module(new WBU()) // Write Back Unit
  val REG = Module(new REG()) // Register File

  // 第一步 REG将pc输出给IFU读取指令 IFU将读取指令传递给GNU，
  io.pc_output <> REG.io.out.pc

  IFU.io.in.valid <> io.inst_input.valid
  IFU.io.in.ready <> io.inst_input.ready
  IFU.io.in.bits.inst <> io.inst_input.bits
  IFU.io.in.bits.pc <> REG.io.out.pc

  GNU.io.in.valid <> IFU.io.out.valid
  GNU.io.in.ready <> IFU.io.out.ready
  GNU.io.in.bits.inst <> IFU.io.out.bits.inst
  GNU.io.in.bits.PC <> IFU.io.out.bits.pc
  GNU.io.out.ready <> 1.U

  // GNU.io.in.valid <> io.inst_input.valid
  // GNU.io.in.ready <> io.inst_input.ready
  // GNU.io.in.bits.inst <> io.inst_input.bits
  // GNU.io.in.bits.PC <> REG.io.out.pc
  // GNU处理完成之后传递给REG读取两个GPR德值并返回给GNU，
  GNU.io.out.bits.inst(19, 15) <> REG.io.in.GPR_raddra
  GNU.io.out.bits.inst(24, 20) <> REG.io.in.GPR_raddrb
  GNU.io.in.bits.GPR_Adata <> REG.io.out.GPR_rdataa
  GNU.io.in.bits.GPR_Bdata <> REG.io.out.GPR_rdatab

  // GNU将控制信号和两个寄存器值传递给EXU，同时根据需要读取的地址将csr寄存器的值传递给EXU
  GNU.io.out.bits.CSR_raddr <> REG.io.in.csr_raddr

  EXU.io.in.RegWr <> GNU.io.out.bits.RegWr
  EXU.io.in.Branch <> GNU.io.out.bits.Branch
  EXU.io.in.MemtoReg <> GNU.io.out.bits.MemtoReg
  EXU.io.in.MemWr <> GNU.io.out.bits.MemWr
  EXU.io.in.MemOp <> GNU.io.out.bits.MemOp
  EXU.io.in.ALUAsrc <> GNU.io.out.bits.ALUAsrc
  EXU.io.in.ALUBsrc <> GNU.io.out.bits.ALUBsrc
  EXU.io.in.ALUctr <> GNU.io.out.bits.ALUctr
  EXU.io.in.csr_ctr <> GNU.io.out.bits.csr_ctr
  EXU.io.in.Imm <> GNU.io.out.bits.Imm
  EXU.io.in.GPR_Adata <> GNU.io.out.bits.GPR_Adata
  EXU.io.in.GPR_Bdata <> GNU.io.out.bits.GPR_Bdata
  EXU.io.in.GPR_waddr <> GNU.io.out.bits.GPR_waddr
  EXU.io.in.PC <> GNU.io.out.bits.PC
  EXU.io.in.CSR <> REG.io.out.csr_rdata

  // 第二步，EXU处理完成之后将结果传递给WBU，WBU根据结果更新系统状态，包括GPR，CSR，PC以及内存
  WBU.io.in.RegWr <> EXU.io.out.RegWr
  WBU.io.in.Branch <> EXU.io.out.Branch
  WBU.io.in.MemtoReg <> EXU.io.out.MemtoReg
  WBU.io.in.MemWr <> EXU.io.out.MemWr
  WBU.io.in.MemOp <> EXU.io.out.MemOp
  WBU.io.in.csr_ctr <> EXU.io.out.csr_ctr
  WBU.io.in.Imm <> EXU.io.out.Imm
  WBU.io.in.GPR_Adata <> EXU.io.out.GPR_Adata
  WBU.io.in.GPR_Bdata <> EXU.io.out.GPR_Bdata
  WBU.io.in.GPR_waddr <> EXU.io.out.GPR_waddr
  WBU.io.in.PC <> EXU.io.out.PC
  WBU.io.in.CSR <> REG.io.out.csr_rdata
  WBU.io.in.Result <> EXU.io.out.Result
  WBU.io.in.Zero <> EXU.io.out.Zero
  WBU.io.in.Less <> EXU.io.out.Less

  WBU.io.in.Mem_rdata <> io.mem_rdata

  REG.io.in.GPR_wdata <> WBU.io.out.GPR_wdata
  REG.io.in.GPR_waddr <> WBU.io.out.GPR_waddr
  REG.io.in.GPR_wen <> WBU.io.out.GPR_wen
  REG.io.in.pc <> WBU.io.out.Next_Pc

  REG.io.in.csr_ctr    := WBU.io.out.CSR_ctr
  REG.io.in.csr_waddra := WBU.io.out.CSR_waddra
  REG.io.in.csr_waddrb := WBU.io.out.CSR_waddrb
  REG.io.in.csr_wdataa := WBU.io.out.CSR_wdataa
  REG.io.in.csr_wdatab := WBU.io.out.CSR_wdatab

  io.mem_wraddr := EXU.io.out.Result
  io.mem_wdata  := EXU.io.out.GPR_Bdata
  io.mem_wop    := GNU.io.out.bits.MemOp
  io.mem_wen    := GNU.io.out.bits.MemWr
}
