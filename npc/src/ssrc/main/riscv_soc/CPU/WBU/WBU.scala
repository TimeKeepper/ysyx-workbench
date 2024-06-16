package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

// riscv writeback unit

class WBU_input extends Bundle {
  val RegWr     = Input(Bool())
  val Branch    = Input(Bran_Type)
  val MemtoReg  = Input(Bool())
  val MemWr     = Input(Bool())
  val MemOp     = Input(MemOp_Type)
  val csr_ctr   = Input(CSR_Type)
  val Imm       = Input(UInt(32.W))
  val GPR_Adata = Input(UInt(32.W))
  val GPR_Bdata = Input(UInt(32.W))
  val GPR_waddr = Input(UInt(5.W))
  val PC        = Input(UInt(32.W))
  val CSR       = Input(UInt(32.W))
  val Result    = Input(UInt(32.W))
  val Zero      = Input(Bool())
  val Less      = Input(Bool())

  val Mem_rdata = Input(UInt(32.W))
}

class WBU_output extends Bundle {
  val Mem_wraddr = Output(UInt(32.W))
  val Mem_wdata  = Output(UInt(32.W))
  val MemOp      = Output(MemOp_Type)
  val MemWr      = Output(Bool())

  val Next_Pc    = Output(UInt(32.W))
  val GPR_waddr  = Output(UInt(5.W))
  val GPR_wdata  = Output(UInt(32.W))
  val GPR_wen    = Output(Bool())
  val CSR_ctr    = Output(CSR_Type)
  val CSR_waddra = Output(UInt(12.W))
  val CSR_waddrb = Output(UInt(12.W))
  val CSR_wdataa = Output(UInt(32.W))
  val CSR_wdatab = Output(UInt(32.W))
}

class WBU extends Module {
  val io = IO(new Bundle {
    val in  = new WBU_input
    val out = new WBU_output
  })

  io.out.Mem_wraddr <> io.in.Result
  io.out.Mem_wdata <> io.in.GPR_Bdata
  io.out.MemOp <> io.in.MemOp
  io.out.MemWr <> io.in.MemWr

  val bcu = Module(new BCU)

  bcu.io.Branch <> io.in.Branch
  bcu.io.Zero <> io.in.Zero
  bcu.io.Less <> io.in.Less

  val PCAsrc = Wire(UInt(32.W))
  val PCBsrc = Wire(UInt(32.W))

  PCAsrc := MuxLookup(bcu.io.PCAsrc, 0.U)(
    Seq(
      PCAsrc_Imm -> io.in.Imm,
      PCAsrc_4 -> 4.U,
      PCAsrc_csr -> io.in.CSR
    )
  )

  PCBsrc := MuxLookup(bcu.io.PCBsrc, 0.U)(
    Seq(
      PCBsrc_gpr -> io.in.GPR_Adata,
      PCBsrc_pc -> io.in.PC,
      PCBsrc_0 -> 0.U
    )
  )

  io.out.Next_Pc := PCAsrc + PCBsrc

  io.out.GPR_waddr := io.in.GPR_waddr
  io.out.GPR_wdata := MuxLookup(io.in.MemtoReg, io.in.Result)(
    Seq(
      Y -> io.in.Mem_rdata,
      N -> io.in.Result
    )
  )
  io.out.GPR_wen <> io.in.RegWr

  io.out.CSR_ctr <> io.in.csr_ctr

  io.out.CSR_waddra := MuxLookup(io.in.csr_ctr, io.in.Imm(11, 0))(
    Seq(
      CSR_R1W2 -> "h341".U
    )
  )

  io.out.CSR_waddrb := "h342".U

  io.out.CSR_wdataa := MuxLookup(io.in.csr_ctr, io.in.GPR_Adata)(
    Seq(
      CSR_R1W2 -> io.in.PC
    )
  )

  io.out.CSR_wdatab := 11.U
}
