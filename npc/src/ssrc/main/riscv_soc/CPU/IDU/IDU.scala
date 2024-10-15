package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._
import Instructions._
// riscv generating number(all meassge ALU and other thing needs) unit

object Decode {
  import signal_value._

  import Instructions._

  // format: off
    val default =
    //   Extop     RegWr  Branch   MemtoReg  MemWr   MemOp       ALUAsrc    ALUBsrc         ALUctr     csr_ctr 
    //     |        |       |         |       |        |           |          |               |          |    
    List(Imm_I,     N,   Bran_NJmp,   N,      N,   MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2,  ALUctr_ADD,   CSR_N)

    val map = Array(
        BitPat(LUI)     -> List(Imm_U, Y, Bran_NJmp, N, N, BitPat.dontCare(3), ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_B,      CSR_N    ),
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

class ysyx_23060198_IDU extends Module{
    val io = IO(new Bundle{
        val IFU_2_IDU     = Flipped(Decoupled(Input(new BUS_IFU_2_IDU)))
        val REG_2_IDU     = Input(new BUS_REG_2_IDU)

        val IDU_2_EXU     = Decoupled(Output(new BUS_IDU_2_EXU))
        val IDU_2_REG     = Output(new BUS_IDU_2_REG)
    })

    val state = RegInit(s_wait_valid)

    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.IFU_2_IDU.valid, s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.IDU_2_EXU.ready, s_wait_valid, s_wait_ready),
        )
    )

    io.IDU_2_EXU.valid := state === s_wait_ready
    io.IFU_2_IDU.ready := state === s_wait_valid
    val comunication_succeed = (io.IFU_2_IDU.valid && io.IFU_2_IDU.ready)

    val ctrlSignals = ListLookup(io.IFU_2_IDU.bits.data, Decode.default, Decode.map)

    val imm = MuxLookup(ctrlSignals(0), 0.U)(
        Seq(
            Imm_I -> Cat(Fill(21, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(31, 20)),
            Imm_U -> Cat(io.IFU_2_IDU.bits.data(31, 12), Fill(12, 0.U)),
            Imm_S -> Cat(Fill(20, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(31, 25), io.IFU_2_IDU.bits.data(11, 7)),
            Imm_B -> Cat(Fill(20, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(7), io.IFU_2_IDU.bits.data(30, 25), io.IFU_2_IDU.bits.data(11, 8), 0.U(1.W)),
            Imm_J -> Cat(Fill(12, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(19, 12), io.IFU_2_IDU.bits.data(20), io.IFU_2_IDU.bits.data(30, 21), 0.U(1.W)),
        )
    )

    val csr_raddr = MuxLookup(ctrlSignals(9), imm(11, 0))(
        Seq(
            CSR_R1W0 -> "h341".U,
            CSR_R1W2 -> "h305".U,
        )
    )

    io.IDU_2_REG.CSR_raddr         <> csr_raddr

    io.IDU_2_EXU.bits.RegWr        <> RegEnable(ctrlSignals(1),         comunication_succeed) 
    io.IDU_2_EXU.bits.Branch       <> RegEnable(ctrlSignals(2),         comunication_succeed) 
    io.IDU_2_EXU.bits.MemtoReg     <> RegEnable(ctrlSignals(3),         comunication_succeed) 
    io.IDU_2_EXU.bits.MemWr        <> RegEnable(ctrlSignals(4),         comunication_succeed) 
    io.IDU_2_EXU.bits.MemOp        <> RegEnable(ctrlSignals(5),         comunication_succeed) 
    io.IDU_2_EXU.bits.ALUAsrc      <> RegEnable(ctrlSignals(6),         comunication_succeed) 
    io.IDU_2_EXU.bits.ALUBsrc      <> RegEnable(ctrlSignals(7),         comunication_succeed) 
    io.IDU_2_EXU.bits.ALUctr       <> RegEnable(ctrlSignals(8),         comunication_succeed) 
    io.IDU_2_EXU.bits.csr_ctr      <> RegEnable(ctrlSignals(9),         comunication_succeed) 
    io.IDU_2_EXU.bits.Imm          <> RegEnable(imm,                    comunication_succeed) 
    io.IDU_2_EXU.bits.GPR_Adata    <> RegEnable(io.REG_2_IDU.GPR_Adata,  comunication_succeed) 
    io.IDU_2_EXU.bits.GPR_Bdata    <> RegEnable(io.REG_2_IDU.GPR_Bdata,  comunication_succeed) 
    io.IDU_2_EXU.bits.GPR_waddr    <> RegEnable(io.IFU_2_IDU.bits.data(11, 7), comunication_succeed) 
    io.IDU_2_EXU.bits.PC           <> RegEnable(io.REG_2_IDU.PC,         comunication_succeed) 
    io.IDU_2_EXU.bits.CSR_rdata    <> RegEnable(io.REG_2_IDU.CSR_rdata,  comunication_succeed) 
}
