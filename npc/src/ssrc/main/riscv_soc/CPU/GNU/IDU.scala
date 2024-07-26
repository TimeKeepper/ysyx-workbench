package riscv_cpu

import chisel3._
import chisel3.util._

object Decode {
  import signal_value._

  import Instructions._

  // format: off
    val default =
    //   Extop     RegWr  Branch   MemtoReg  MemWr   MemOp       ALUAsrc    ALUBsrc         ALUctr     csr_ctr 
    //     |        |       |         |       |        |           |          |               |          |    
    List(Imm_I,     N,   Bran_NJmp,   N,      N,   MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2,  ALUctr_ADD,   CSR_N)

    val map = Array(
        BitPat(LUI)     -> List(Imm_U, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_B,      CSR_N    ),
        BitPat(AUIPC)   -> List(Imm_U, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_PC,  ALUBSrc_IMM, ALUctr_ADD,    CSR_N    ),
        BitPat(ADDI)    -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_ADD,    CSR_N    ),
        BitPat(SLTI)    -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_Less_S, CSR_N    ),
        BitPat(SLTIU)   -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_Less_U, CSR_N    ),
        BitPat(XORI)    -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_XOR,    CSR_N    ),
        BitPat(ORI)     -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_OR,     CSR_N    ),
        BitPat(ANDI)    -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_AND,    CSR_N    ),
        BitPat(SLLI)    -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_SLL,    CSR_N    ),
        BitPat(SRLI)    -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_SRL,    CSR_N    ),
        BitPat(SRAI)    -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_SRA,    CSR_N    ),
        BitPat(ADD)     -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_ADD,    CSR_N    ),
        BitPat(SUB)     -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_SUB,    CSR_N    ),
        BitPat(SLL)     -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_SLL,    CSR_N    ),
        BitPat(SLT)     -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_Less_S, CSR_N    ),
        BitPat(SLTU)    -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_Less_U, CSR_N    ),
        BitPat(XOR)     -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_XOR,    CSR_N    ),
        BitPat(SRL)     -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_SRL,    CSR_N    ),
        BitPat(SRA)     -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_SRA,    CSR_N    ),
        BitPat(OR)      -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_OR,     CSR_N    ),
        BitPat(AND)     -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_AND,    CSR_N    ),
        BitPat(JAL)     -> List(Imm_J, Y, Bran_Jmp,  N, N, MemOp_1BS, ALUAsrc_PC,  ALUBSrc_4,   ALUctr_ADD,    CSR_N    ),
        BitPat(JALR)    -> List(Imm_I, Y, Bran_Jmpr, N, N, MemOp_1BS, ALUAsrc_PC,  ALUBSrc_4,   ALUctr_ADD,    CSR_N    ),
        BitPat(BEQ)     -> List(Imm_B, N, Bran_Jeq,  N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_Less_S, CSR_N    ),
        BitPat(BNE)     -> List(Imm_B, N, Bran_Jne,  N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_Less_S, CSR_N    ),
        BitPat(BLT)     -> List(Imm_B, N, Bran_Jlt,  N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_Less_S, CSR_N    ),
        BitPat(BGE)     -> List(Imm_B, N, Bran_Jge,  N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_Less_S, CSR_N    ),
        BitPat(BLTU)    -> List(Imm_B, N, Bran_Jlt,  N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_Less_U, CSR_N    ),
        BitPat(BGEU)    -> List(Imm_B, N, Bran_Jge,  N, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_Less_U, CSR_N    ),
        BitPat(LB)      -> List(Imm_I, Y, Bran_NJmp, Y, N, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(LH)      -> List(Imm_I, Y, Bran_NJmp, Y, N, MemOp_2BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(LW)      -> List(Imm_I, Y, Bran_NJmp, Y, N, MemOp_4BU, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(LBU)     -> List(Imm_I, Y, Bran_NJmp, Y, N, MemOp_1BU, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(LHU)     -> List(Imm_I, Y, Bran_NJmp, Y, N, MemOp_2BU, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(SB)      -> List(Imm_S, N, Bran_NJmp, N, Y, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(SH)      -> List(Imm_S, N, Bran_NJmp, N, Y, MemOp_2BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(SW)      -> List(Imm_S, N, Bran_NJmp, N, Y, MemOp_4BU, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(CSRRW)   -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_CSR, ALUBSrc_RS1, ALUctr_B,      CSR_R1W1 ),
        BitPat(CSRRS)   -> List(Imm_I, Y, Bran_NJmp, N, N, MemOp_1BS, ALUAsrc_CSR, ALUBSrc_RS1, ALUctr_OR,     CSR_R1W1 ),
        BitPat(ECALL)   -> List(Imm_I, N, Bran_Jcsr, N, N, MemOp_1BS, ALUAsrc_CSR, ALUBSrc_RS1, ALUctr_ADD,    CSR_R1W2 ),
        BitPat(MRET)    -> List(Imm_I, N, Bran_Jcsr, N, N, MemOp_1BS, ALUAsrc_CSR, ALUBSrc_RS1, ALUctr_ADD,    CSR_R1W0 )
    )
    // format: on
}

// riscv cpu instruction decode unit

class ysyx_23060198_IDU extends Module {
  import signal_value._
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))

    val ExtOp    = Output(Imm_Type)
    val RegWr    = Output(Bool())
    val Branch   = Output(Bran_Type)
    val MemtoReg = Output(Bool())
    val MemWr    = Output(Bool())
    val MemOp    = Output(MemOp_Type)
    val ALUAsrc  = Output(ALUAsrc_Type)
    val ALUBsrc  = Output(ALUBSrc_Type)
    val ALUctr   = Output(ALUctr_Type)
    val csr_ctr  = Output(CSR_Type)
  })

  val ctrlSignals = ListLookup(io.inst, Decode.default, Decode.map)

  io.ExtOp        := ctrlSignals(0)
  io.RegWr        := ctrlSignals(1)
  io.Branch       := ctrlSignals(2)
  io.MemtoReg     := ctrlSignals(3)
  io.MemWr        := ctrlSignals(4)
  io.MemOp        := ctrlSignals(5)
  io.ALUAsrc      := ctrlSignals(6)
  io.ALUBsrc      := ctrlSignals(7)
  io.ALUctr       := ctrlSignals(8)
  io.csr_ctr      := ctrlSignals(9)
}
