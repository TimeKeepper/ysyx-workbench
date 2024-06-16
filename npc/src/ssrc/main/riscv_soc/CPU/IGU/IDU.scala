package riscv_cpu

import chisel3._
import chisel3.util._

object Decode {
  import signal_value._

  import Instructions._

  // format: off
    val default =
    //   Extop     RegWr  Branch   MemtoReg  MemWr   MemOp     ALUAsrc   ALUBsrc   ALUctr     csr_ctr 
    //     |        |       |         |       |        |         |         |         |          |    
    List(immI,     N,   Bran_NJmp,    N,      N,    M_1BS,   A_RS1,     B_RS2,  ALU_ADD,    CSR_N)

    val map = Array(
        BitPat(LUI)     -> List(immU, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_B,      CSR_N    ),
        BitPat(AUIPC)   -> List(immU, Y, Bran_NJmp, N, N, M_1BS, A_PC,  B_IMM, ALU_ADD,    CSR_N    ),
        BitPat(ADDI)    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        BitPat(SLTI)    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_Less_S, CSR_N    ),
        BitPat(SLTIU)   -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_Less_U, CSR_N    ),
        BitPat(XORI)    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_XOR,    CSR_N    ),
        BitPat(ORI)     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_OR,     CSR_N    ),
        BitPat(ANDI)    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_AND,    CSR_N    ),
        BitPat(SLLI)    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_SLL,    CSR_N    ),
        BitPat(SRLI)    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_SRL,    CSR_N    ),
        BitPat(SRAI)    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_SRA,    CSR_N    ),
        BitPat(ADD)     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_ADD,    CSR_N    ),
        BitPat(SUB)     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_SUB,    CSR_N    ),
        BitPat(SLL)     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_SLL,    CSR_N    ),
        BitPat(SLT)     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_Less_S, CSR_N    ),
        BitPat(SLTU)    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_Less_U, CSR_N    ),
        BitPat(XOR)     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_XOR,    CSR_N    ),
        BitPat(SRL)     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_SRL,    CSR_N    ),
        BitPat(SRA)     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_SRA,    CSR_N    ),
        BitPat(OR)      -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_OR,     CSR_N    ),
        BitPat(AND)     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_AND,    CSR_N    ),
        BitPat(JAL)     -> List(immJ, Y, Bran_Jmp,  N, N, M_1BS, A_PC,  B_4,   ALU_ADD,    CSR_N    ),
        BitPat(JALR)    -> List(immI, Y, Bran_Jmpr, N, N, M_1BS, A_PC,  B_4,   ALU_ADD,    CSR_N    ),
        BitPat(BEQ)     -> List(immB, N, Bran_Jeq,  N, N, M_1BS, A_RS1, B_RS2, ALU_Less_S, CSR_N    ),
        BitPat(BNE)     -> List(immB, N, Bran_Jne,  N, N, M_1BS, A_RS1, B_RS2, ALU_Less_S, CSR_N    ),
        BitPat(BLT)     -> List(immB, N, Bran_Jlt,  N, N, M_1BS, A_RS1, B_RS2, ALU_Less_S, CSR_N    ),
        BitPat(BGE)     -> List(immB, N, Bran_Jge,  N, N, M_1BS, A_RS1, B_RS2, ALU_Less_S, CSR_N    ),
        BitPat(BLTU)    -> List(immB, N, Bran_Jlt,  N, N, M_1BS, A_RS1, B_RS2, ALU_Less_U, CSR_N    ),
        BitPat(BGEU)    -> List(immB, N, Bran_Jge,  N, N, M_1BS, A_RS1, B_RS2, ALU_Less_U, CSR_N    ),
        BitPat(LB)      -> List(immI, Y, Bran_NJmp, Y, N, M_1BS, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        BitPat(LH)      -> List(immI, Y, Bran_NJmp, Y, N, M_2BS, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        BitPat(LW)      -> List(immI, Y, Bran_NJmp, Y, N, M_4BU, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        BitPat(LBU)     -> List(immI, Y, Bran_NJmp, Y, N, M_1BU, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        BitPat(LHU)     -> List(immI, Y, Bran_NJmp, Y, N, M_2BU, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        BitPat(SB)      -> List(immS, N, Bran_NJmp, N, Y, M_1BS, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        BitPat(SH)      -> List(immS, N, Bran_NJmp, N, Y, M_2BS, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        BitPat(SW)      -> List(immS, N, Bran_NJmp, N, Y, M_4BU, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        BitPat(CSRRW)   -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_CSR, B_RS1, ALU_A,      CSR_R1W1 ),
        BitPat(CSRRS)   -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_CSR, B_RS1, ALU_OR,     CSR_R1W1 ),
        BitPat(ECALL)   -> List(immI, N, Bran_Jcsr, N, N, M_1BS, A_CSR, B_RS1, ALU_ADD,    CSR_R1W2 ),
        BitPat(MRET)    -> List(immI, N, Bran_Jcsr, N, N, M_1BS, A_CSR, B_RS1, ALU_ADD,    CSR_R1W0 )
    )
    // format: on
}

// riscv cpu instruction decode unit

class IDU extends Module {
  import signal_value._
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))

    val ExtOp    = Output(ExtOp_Type)
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
