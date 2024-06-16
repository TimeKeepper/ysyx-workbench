package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import Instructions._
// riscv generating number(all meassge ALU and other thing needs) unit

class GNU_input extends Bundle {
  val inst      = Input(UInt(32.W))
  val GPR_Adata = Input(UInt(32.W))
  val GPR_Bdata = Input(UInt(32.W))
  val PC        = Input(UInt(32.W))
}

class GNU_output extends Bundle {
  val inst      = Output(UInt(32.W))
  val RegWr     = Output(Bool())
  val Branch    = Output(Bran_Type)
  val MemtoReg  = Output(Bool())
  val MemWr     = Output(Bool())
  val MemOp     = Output(MemOp_Type)
  val ALUAsrc   = Output(ALUAsrc_Type)
  val ALUBsrc   = Output(ALUBSrc_Type)
  val ALUctr    = Output(ALUctr_Type)
  val csr_ctr   = Output(CSR_Type)
  val Imm       = Output(UInt(32.W))
  val GPR_Adata = Output(UInt(32.W))
  val GPR_Bdata = Output(UInt(32.W))
  val GPR_waddr = Output(UInt(5.W))
  val PC        = Output(UInt(32.W))
  val CSR_raddr = Output(UInt(12.W))
}

class GNU extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new GNU_input))
    val out = Decoupled(new GNU_output)
  })

  val s_wait_valid :: s_wait_ready :: Nil = Enum(2)
  val state                               = RegInit(s_wait_valid)

  io.in.ready  := (state === s_wait_valid)
  io.out.valid := (state === s_wait_ready)

  state := MuxLookup(state, s_wait_valid)(
    Seq(
      s_wait_valid -> Mux(io.in.valid, s_wait_ready, s_wait_valid),
      s_wait_ready -> Mux(io.out.ready, s_wait_valid, s_wait_ready)
    )
  )

  val idu = Module(new IDU)
  val igu = Module(new IGU)


  idu.io.inst <> io.in.bits.inst
  idu.io.RegWr <> io.out.bits.RegWr
  io.out.bits.Branch <> idu.io.Branch
  idu.io.MemtoReg <> io.out.bits.MemtoReg
  idu.io.MemWr <> io.out.bits.MemWr
  idu.io.MemOp <> io.out.bits.MemOp
  idu.io.ALUAsrc <> io.out.bits.ALUAsrc
  idu.io.ALUBsrc <> io.out.bits.ALUBsrc
  idu.io.ALUctr <> io.out.bits.ALUctr
  idu.io.csr_ctr <> io.out.bits.csr_ctr

  igu.io.inst <> io.in.bits.inst
  igu.io.ExtOp <> idu.io.ExtOp
  igu.io.imm <> io.out.bits.Imm

  io.out.bits.GPR_Adata <> io.in.bits.GPR_Adata
  io.out.bits.GPR_Bdata <> io.in.bits.GPR_Bdata
  io.out.bits.GPR_waddr <> inst(11, 7)
  io.out.bits.PC <> io.in.bits.PC
  io.out.bits.inst <> io.in.bits.inst

  io.out.bits.CSR_raddr := MuxLookup(io.out.bits.csr_ctr, io.out.bits.Imm(11, 0))(
    Seq(
      CSR_R1W0 -> "h341".U,
      CSR_R1W2 -> "h305".U
    )
  )
  // when(io.out.csr_ctr === CSR_R1W0) {
  //     CSR_RADDR := "h341".U // instruction mret read mepc to recovered pc
  // }.elsewhen(GNU.io.out.csr_ctr === CSR_R1W2) {
  //     CSR_RADDR := "h305".U // instruction ecall read mtevc to get to error order function
  // }.otherwise {
  //     CSR_RADDR := GNU.io.out.Imm(11, 0)
  // }
}
