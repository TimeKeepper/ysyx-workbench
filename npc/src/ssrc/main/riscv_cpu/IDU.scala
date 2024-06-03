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
        LUI     -> List(immU, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_B,      CSR_N    ),
        AUIPC   -> List(immU, Y, Bran_NJmp, N, N, M_1BS, A_PC,  B_IMM, ALU_ADD,    CSR_N    ),
        ADDI    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        SLTI    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_Less_S, CSR_N    ),
        SLTIU   -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_Less_U, CSR_N    ),
        XORI    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_XOR,    CSR_N    ),
        ORI     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_OR,     CSR_N    ),
        ANDI    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_AND,    CSR_N    ),
        SLLI    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_SLL,    CSR_N    ),
        SRLI    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_SRL,    CSR_N    ),
        SRAI    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_IMM, ALU_SRA,    CSR_N    ),
        ADD     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_ADD,    CSR_N    ),
        SUB     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_SUB,    CSR_N    ),
        SLL     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_SLL,    CSR_N    ),
        SLT     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_Less_S, CSR_N    ),
        SLTU    -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_Less_U, CSR_N    ),
        XOR     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_XOR,    CSR_N    ),
        SRL     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_SRL,    CSR_N    ),
        SRA     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_SRA,    CSR_N    ),
        OR      -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_OR,     CSR_N    ),
        AND     -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_RS1, B_RS2, ALU_AND,    CSR_N    ),
        JAL     -> List(immJ, Y, Bran_Jmp,  N, N, M_1BS, A_PC,  B_4,   ALU_ADD,    CSR_N    ),
        JALR    -> List(immI, Y, Bran_Jmpr, N, N, M_1BS, A_PC,  B_4,   ALU_ADD,    CSR_N    ),
        BEQ     -> List(immB, N, Bran_Jeq,  N, N, M_1BS, A_RS1, B_RS2, ALU_Less_S, CSR_N    ),
        BNE     -> List(immB, N, Bran_Jne,  N, N, M_1BS, A_RS1, B_RS2, ALU_Less_S, CSR_N    ),
        BLT     -> List(immB, N, Bran_Jlt,  N, N, M_1BS, A_RS1, B_RS2, ALU_Less_S, CSR_N    ),
        BGE     -> List(immB, N, Bran_Jge,  N, N, M_1BS, A_RS1, B_RS2, ALU_Less_S, CSR_N    ),
        BLTU    -> List(immB, N, Bran_Jlt,  N, N, M_1BS, A_RS1, B_RS2, ALU_Less_U, CSR_N    ),
        BGEU    -> List(immB, N, Bran_Jge,  N, N, M_1BS, A_RS1, B_RS2, ALU_Less_U, CSR_N    ),
        LB      -> List(immI, Y, Bran_NJmp, Y, N, M_1BS, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        LH      -> List(immI, Y, Bran_NJmp, Y, N, M_2BS, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        LW      -> List(immI, Y, Bran_NJmp, Y, N, M_4BU, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        LBU     -> List(immI, Y, Bran_NJmp, Y, N, M_1BU, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        LHU     -> List(immI, Y, Bran_NJmp, Y, N, M_2BU, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        SB      -> List(immS, N, Bran_NJmp, N, Y, M_1BS, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        SH      -> List(immS, N, Bran_NJmp, N, Y, M_2BS, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        SW      -> List(immS, N, Bran_NJmp, N, Y, M_4BU, A_RS1, B_IMM, ALU_ADD,    CSR_N    ),
        CSRRW   -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_CSR, B_RS1, ALU_A,      CSR_R1W1 ),
        CSRRS   -> List(immI, Y, Bran_NJmp, N, N, M_1BS, A_CSR, B_RS1, ALU_OR,     CSR_R1W1 ),
        ECALL   -> List(immI, N, Bran_Jcsr, N, N, M_1BS, A_CSR, B_RS1, ALU_ADD,    CSR_R1W2 ),
        MRET    -> List(immI, N, Bran_Jcsr, N, N, M_1BS, A_CSR, B_RS1, ALU_ADD,    CSR_R1W0 )
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
    val Branch   = Decoupled(Bran_Type)
    val MemtoReg = Output(Bool())
    val MemWr    = Output(Bool())
    val MemOp    = Output(MemOp_Type)
    val ALUAsrc  = Output(ALUAsrc_Type)
    val ALUBsrc  = Output(ALUBSrc_Type)
    val ALUctr   = Output(ALUctr_Type)
    val csr_ctr  = Output(CSR_Type)
  })

  val ctrlSignals = ListLookup(io.inst, Decode.default, Decode.map)

  io.ExtOp    := ctrlSignals(0)
  io.RegWr    := ctrlSignals(1)
  io.Branch.valid := true.B
  io.Branch.bits  := ctrlSignals(2)
  io.MemtoReg := ctrlSignals(3)
  io.MemWr    := ctrlSignals(4)
  io.MemOp    := ctrlSignals(5)
  io.ALUAsrc  := ctrlSignals(6)
  io.ALUBsrc  := ctrlSignals(7)
  io.ALUctr   := ctrlSignals(8)
  io.csr_ctr  := ctrlSignals(9)
}
