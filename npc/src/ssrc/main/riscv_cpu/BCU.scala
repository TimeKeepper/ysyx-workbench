package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

// riscv cpu branch control unit

class BCU extends Module {
  val io = IO(new Bundle {
    val Branch = Flipped(Decoupled(Bran_Type))
    val Zero   = Input(Bool())
    val Less   = Input(Bool())

    val PCAsrc = Output(PCAsrc_Type)
    val PCBsrc = Output(PCBsrc_Type)
  })

  io.Branch.ready := true.B

  io.PCAsrc := MuxLookup(io.Branch.bits, PCAsrc_4)(
    Seq(
      Bran_Jmp -> PCAsrc_Imm,
      Bran_Jmpr -> PCAsrc_Imm,
      Bran_Jeq -> Mux(io.Zero, PCAsrc_Imm, PCAsrc_4),
      Bran_Jne -> Mux(io.Zero, PCAsrc_4, PCAsrc_Imm),
      Bran_Jlt -> Mux(io.Less, PCAsrc_Imm, PCAsrc_4),
      Bran_Jge -> Mux(io.Less, PCAsrc_4, PCAsrc_Imm),
      Bran_Jcsr -> PCAsrc_csr
    )
  )

  io.PCBsrc := MuxLookup(io.Branch.bits, PCBsrc_pc)(
    Seq(
      Bran_Jmpr -> PCBsrc_gpr,
      Bran_Jcsr -> PCBsrc_0
    )
  )
}
