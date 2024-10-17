package riscv_cpu

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._

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
    List(Bran_NJmp,   MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2,  ALUctr_ADD,   CSR_N)

    val map = Array(
        BitPat(LUI)     -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_B,      CSR_N    ),
        BitPat(AUIPC)   -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_PC,  ALUBSrc_IMM, ALUctr_ADD,    CSR_N    ),
        BitPat(ADDI)    -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_ADD,    CSR_N    ),
        BitPat(SLTI)    -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_Less_S, CSR_N    ),
        BitPat(SLTIU)   -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_Less_U, CSR_N    ),
        BitPat(XORI)    -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_XOR,    CSR_N    ),
        BitPat(ORI)     -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_OR,     CSR_N    ),
        BitPat(ANDI)    -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_AND,    CSR_N    ),
        BitPat(SLLI)    -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_SLL,    CSR_N    ),
        BitPat(SRLI)    -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_SRL,    CSR_N    ),
        BitPat(SRAI)    -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUctr_SRA,    CSR_N    ),
        BitPat(ADD)     -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_ADD,    CSR_N    ),
        BitPat(SUB)     -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_SUB,    CSR_N    ),
        BitPat(SLL)     -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_SLL,    CSR_N    ),
        BitPat(SLT)     -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_Less_S, CSR_N    ),
        BitPat(SLTU)    -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_Less_U, CSR_N    ),
        BitPat(XOR)     -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_XOR,    CSR_N    ),
        BitPat(SRL)     -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_SRL,    CSR_N    ),
        BitPat(SRA)     -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_SRA,    CSR_N    ),
        BitPat(OR)      -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_OR,     CSR_N    ),
        BitPat(AND)     -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_AND,    CSR_N    ),
        BitPat(JAL)     -> List(Bran_Jmp,  MemOp_1BS, ALUAsrc_PC,  ALUBSrc_4,   ALUctr_ADD,    CSR_N    ),
        BitPat(JALR)    -> List(Bran_Jmpr, MemOp_1BS, ALUAsrc_PC,  ALUBSrc_4,   ALUctr_ADD,    CSR_N    ),
        BitPat(BEQ)     -> List(Bran_Jeq,  MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_SUB,    CSR_N    ),
        BitPat(BNE)     -> List(Bran_Jne,  MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_SUB,    CSR_N    ),
        BitPat(BLT)     -> List(Bran_Jlt,  MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_Less_S, CSR_N    ),
        BitPat(BGE)     -> List(Bran_Jge,  MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_Less_S, CSR_N    ),
        BitPat(BLTU)    -> List(Bran_Jlt,  MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_Less_U, CSR_N    ),
        BitPat(BGEU)    -> List(Bran_Jge,  MemOp_1BS, ALUAsrc_RS1, ALUBSrc_RS2, ALUctr_Less_U, CSR_N    ),
        BitPat(LB)      -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(LH)      -> List(Bran_NJmp, MemOp_2BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(LW)      -> List(Bran_NJmp, MemOp_4BU, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(LBU)     -> List(Bran_NJmp, MemOp_1BU, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(LHU)     -> List(Bran_NJmp, MemOp_2BU, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(SB)      -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(SH)      -> List(Bran_NJmp, MemOp_2BS, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(SW)      -> List(Bran_NJmp, MemOp_4BU, ALUAsrc_RS1, ALUBSrc_IMM, ALUAsrc_RS1,   CSR_N    ),
        BitPat(CSRRW)   -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_CSR, ALUBSrc_RS1, ALUctr_B,      CSR_R1W1 ),
        BitPat(CSRRS)   -> List(Bran_NJmp, MemOp_1BS, ALUAsrc_CSR, ALUBSrc_RS1, ALUctr_OR,     CSR_R1W1 ),
        BitPat(ECALL)   -> List(Bran_Jcsr, MemOp_1BS, ALUAsrc_CSR, ALUBSrc_RS1, ALUctr_ADD,    CSR_R1W2 ),
        BitPat(MRET)    -> List(Bran_Jcsr, MemOp_1BS, ALUAsrc_CSR, ALUBSrc_RS1, ALUctr_ADD,    CSR_R1W0 )
    )
    // format: on
}

case class InstructionPattern(
    val func7: BitPat = BitPat.dontCare(7),
    val func3: BitPat = BitPat.dontCare(3),
    val opcode: BitPat
) extends DecodePattern {
    def bitPat: BitPat = func7 ## BitPat.dontCare(10) ## func3 ## BitPat.dontCare(5) ## opcode
}

object ImmField extends DecodeField[InstructionPattern, UInt] {
    def name: String = "imm"
    def chiselType = Imm_Type
    def genTable(op: InstructionPattern): BitPat = {
        op.opcode.rawString match {
            case "0000011" => BitPat(Imm_I) // Loadxx
            case "0100011" => BitPat(Imm_S) // Storexx
            case "0010011" => BitPat(Imm_I) // xxI
            case "0110111" => BitPat(Imm_U) // LUI
            case "0010111" => BitPat(Imm_U) // AUIPC
            case "1100011" => BitPat(Imm_B) // Branchxx
            case "1101111" => BitPat(Imm_J) // JAL
            case "1100111" => BitPat(Imm_I) // JALR
            case "1110011" => BitPat(Imm_I) // CSRRx
            case _ => BitPat.dontCare(Imm_width)
        }
    }
}

object RegWrFiled extends BoolDecodeField[InstructionPattern] {
    def name: String = "RegWr"
    def genTable(op: InstructionPattern): BitPat = {
        op.opcode.rawString match {
            case "0000011" => BitPat(Y) 
            case "0100011" => BitPat(N)
            case "0110011" => BitPat(Y)
            case "0010011" => BitPat(Y)
            case "0110111" => BitPat(Y)
            case "0010111" => BitPat(Y)
            case "1100011" => BitPat(N)
            case "1101111" => BitPat(Y)
            case "1100111" => BitPat(Y)
            case "1110011" => if (op.func3 != BitPat("b000")) BitPat(Y) else BitPat(N)
            case _ => BitPat.dontCare(1)
        }
    }
}

object BranchField extends DecodeField[InstructionPattern, UInt] {
    def name: String = "Branch"
    def chiselType = Bran_Type
    
    def genTable(op: InstructionPattern): BitPat = {
        (op.opcode ## op.func3).rawString match {
            case "1100011000" => BitPat(Bran_Jeq)
            case "1100011001" => BitPat(Bran_Jne)
            case "1100011100" => BitPat(Bran_Jlt)
            case "1100011101" => BitPat(Bran_Jge)
            case "1100011110" => BitPat(Bran_Jlt)
            case "1100011111" => BitPat(Bran_Jge)
            case "1101111???" => BitPat(Bran_Jmp)
            case "1100111000" => BitPat(Bran_Jmpr)
            case "1110011000" => BitPat(Bran_Jcsr)
            case _ => BitPat.dontCare(Bran_width)
        }
    }
}

object MemtoRegField extends BoolDecodeField[InstructionPattern] {
    def name: String = "MemtoReg"
    def genTable(op: InstructionPattern): BitPat = {
        op.opcode.rawString match {
            case "0000011" => BitPat(Y)
            case _ => BitPat(N)
        }
    }
}

object MemWrField extends BoolDecodeField[InstructionPattern] {
    def name: String = "MemWr"
    def genTable(op: InstructionPattern): BitPat = {
        op.opcode.rawString match {
            case "0100011" => BitPat(Y)
            case _ => BitPat(N)
        }
    }
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

    val possiblePattern = Seq(
       InstructionPattern(opcode = BitPat("b0000011")), // Loadxx
       InstructionPattern(opcode = BitPat("b0100011")), // Storexx
       InstructionPattern(opcode = BitPat("b0110011")), // Rtype
       InstructionPattern(opcode = BitPat("b0010011")), // xxI
       InstructionPattern(opcode = BitPat("b0110111")), // LUI
       InstructionPattern(opcode = BitPat("b0010111")), // AUIPC
       InstructionPattern(opcode = BitPat("b1100011")), // Branchxx
       InstructionPattern(opcode = BitPat("b1101111")), // JAL
       InstructionPattern(opcode = BitPat("b1100111")), // JALR
       InstructionPattern(opcode = BitPat("b1110011")), // CSRRx
    )

    val allFields = Seq(
        ImmField,
        RegWrFiled,
        BranchField,
        MemtoRegField,
        MemWrField,
    )

    val decodeTable = new DecodeTable(possiblePattern, allFields)
    val decodeResult = decodeTable.decode(io.IFU_2_IDU.bits.data)

    val imm = MuxLookup(decodeResult(ImmField), 0.U)(
        Seq(
            Imm_I -> Cat(Fill(21, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(31, 20)),
            Imm_U -> Cat(io.IFU_2_IDU.bits.data(31, 12), Fill(12, 0.U)),
            Imm_S -> Cat(Fill(20, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(31, 25), io.IFU_2_IDU.bits.data(11, 7)),
            Imm_B -> Cat(Fill(20, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(7), io.IFU_2_IDU.bits.data(30, 25), io.IFU_2_IDU.bits.data(11, 8), 0.U(1.W)),
            Imm_J -> Cat(Fill(12, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(19, 12), io.IFU_2_IDU.bits.data(20), io.IFU_2_IDU.bits.data(30, 21), 0.U(1.W)),
        )
    )

    val csr_raddr = MuxLookup(ctrlSignals(6), imm(11, 0))(
        Seq(
            CSR_R1W0 -> "h341".U,
            CSR_R1W2 -> "h305".U,
        )
    )

    io.IDU_2_REG.CSR_raddr         <> csr_raddr

    io.IDU_2_EXU.bits.RegWr        <> RegEnable(decodeResult(RegWrFiled), comunication_succeed) 
    io.IDU_2_EXU.bits.Branch       <> RegEnable(ctrlSignals(0), comunication_succeed) 
    io.IDU_2_EXU.bits.MemtoReg     <> RegEnable(decodeResult(MemtoRegField),         comunication_succeed) 
    io.IDU_2_EXU.bits.MemWr        <> RegEnable(decodeResult(MemWrField),         comunication_succeed) 
    io.IDU_2_EXU.bits.MemOp        <> RegEnable(ctrlSignals(2),         comunication_succeed) 
    io.IDU_2_EXU.bits.ALUAsrc      <> RegEnable(ctrlSignals(3),         comunication_succeed) 
    io.IDU_2_EXU.bits.ALUBsrc      <> RegEnable(ctrlSignals(4),         comunication_succeed) 
    io.IDU_2_EXU.bits.ALUctr       <> RegEnable(ctrlSignals(5),         comunication_succeed) 
    io.IDU_2_EXU.bits.csr_ctr      <> RegEnable(ctrlSignals(6),         comunication_succeed) 
    io.IDU_2_EXU.bits.Imm          <> RegEnable(imm,                    comunication_succeed) 
    io.IDU_2_EXU.bits.GPR_Adata    <> RegEnable(io.REG_2_IDU.GPR_Adata,  comunication_succeed) 
    io.IDU_2_EXU.bits.GPR_Bdata    <> RegEnable(io.REG_2_IDU.GPR_Bdata,  comunication_succeed) 
    io.IDU_2_EXU.bits.GPR_waddr    <> RegEnable(io.IFU_2_IDU.bits.data(11, 7), comunication_succeed) 
    io.IDU_2_EXU.bits.PC           <> RegEnable(io.REG_2_IDU.PC,         comunication_succeed) 
    io.IDU_2_EXU.bits.CSR_rdata    <> RegEnable(io.REG_2_IDU.CSR_rdata,  comunication_succeed) 
}
