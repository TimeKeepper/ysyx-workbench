package riscv_cpu

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import org.chipsalliance.rvdecoderdb

import signal_value._
import bus_state._
// riscv generating number(all meassge ALU and other thing needs) unit

trait DecodeAPI {
    def Get_BitPat[T <: Data](Enum: T): BitPat = {
        BitPat(Enum.litValue.U(Enum.getWidth.W))
    }
}

case class rvInstructionPattern(val inst: rvdecoderdb.Instruction) extends DecodePattern {
    override def bitPat: BitPat = BitPat("b" + inst.encoding.toString())
}

object Imm_Field extends DecodeField[rvInstructionPattern, Imm_TypeEnum.Type] with DecodeAPI{
    override def name: String = "imm"
    override def chiselType = Imm_TypeEnum()
    override def genTable(i: rvInstructionPattern): BitPat = {
        i.inst.args.map(_.toString).collectFirst {
            case "imm12" | "shamtw" | "csr"     => Get_BitPat(Imm_TypeEnum.Imm_I)
            case "imm12hi" | "imm12lo"          => Get_BitPat(Imm_TypeEnum.Imm_S)
            case "bimm12hi" | "bimm12lo"        => Get_BitPat(Imm_TypeEnum.Imm_B)
            case "imm20"                        => Get_BitPat(Imm_TypeEnum.Imm_U)
            case "jimm20"                       => Get_BitPat(Imm_TypeEnum.Imm_J)
        }.getOrElse(BitPat.dontCare(Imm_TypeEnum.getWidth))
    }
}

object Bran_Field extends DecodeField[rvInstructionPattern, Bran_TypeEnum.Type] with DecodeAPI {
    override def name: String = "branch"
    override def chiselType = Bran_TypeEnum()
    override def genTable(i: rvInstructionPattern): BitPat = {
        i.inst.name match {
            case "beq"      => Get_BitPat(Bran_TypeEnum.Bran_Jeq)
            case "bne"      => Get_BitPat(Bran_TypeEnum.Bran_Jne)
            case "blt"      => Get_BitPat(Bran_TypeEnum.Bran_Jlt)
            case "bge"      => Get_BitPat(Bran_TypeEnum.Bran_Jge)
            case "bltu"     => Get_BitPat(Bran_TypeEnum.Bran_Jlt)
            case "bgeu"     => Get_BitPat(Bran_TypeEnum.Bran_Jge)
            case "jal"      => Get_BitPat(Bran_TypeEnum.Bran_Jmp)
            case "jalr"     => Get_BitPat(Bran_TypeEnum.Bran_Jmpr)
            case "ecall"    => Get_BitPat(Bran_TypeEnum.Bran_Jcsr)
            case "mret"     => Get_BitPat(Bran_TypeEnum.Bran_Jcsr)
            case _          => Get_BitPat(Bran_TypeEnum.Bran_NJmp)
        }
    }
}

object RegWr_Field extends DecodeField[rvInstructionPattern, RegWr_TypeEnum.Type] with DecodeAPI {
    override def name: String = "regwr"
    override def chiselType = RegWr_TypeEnum()
    override def genTable(i: rvInstructionPattern): BitPat = {
        i.inst.args.map(_.toString).collectFirst {
            case "rd" => Get_BitPat(RegWr_TypeEnum.RegWr_Yes)
        }.getOrElse(Get_BitPat(RegWr_TypeEnum.RegWr_No))
    }
}

object EXUAsrc_Field extends DecodeField[rvInstructionPattern, EXUAsrc_TypeEnum.Type] with DecodeAPI {
    override def name: String = "EXUAsrc"
    override def chiselType = EXUAsrc_TypeEnum()
    override def genTable(i: rvInstructionPattern): BitPat = {
        i.inst.name match {
            case    "jal" | "jalr" | "auipc"                            => Get_BitPat(EXUAsrc_TypeEnum.EXUAsrc_PC)
            case _ => i.inst.args.map(_.toString).collectFirst {
                case "rs1" => Get_BitPat(EXUAsrc_TypeEnum.EXUAsrc_RS1)
            }.getOrElse(BitPat.dontCare(EXUAsrc_TypeEnum.getWidth))
        }
    }
}

object EXUBsrc_Field extends DecodeField[rvInstructionPattern, EXUBsrc_TypeEnum.Type] with DecodeAPI {
    override def name: String = "EXUBsrc"
    override def chiselType = EXUBsrc_TypeEnum()
    override def genTable(i: rvInstructionPattern): BitPat = {
        i.inst.name match {
            case "jal" | "jalr" => Get_BitPat(EXUBsrc_TypeEnum.EXUBsrc_4)
            case _ => i.inst.args.map(_.toString).collectFirst {
                case "rs2" => Get_BitPat(EXUBsrc_TypeEnum.EXUBsrc_RS2)
                case "imm12" | "imm20" => Get_BitPat(EXUBsrc_TypeEnum.EXUBsrc_IMM)
                case "csr" => Get_BitPat(EXUBsrc_TypeEnum.EXUBsrc_CSR)
            }.getOrElse(BitPat.dontCare(EXUBsrc_TypeEnum.getWidth))
        }
    }
}

object EXUctr_Field extends DecodeField[rvInstructionPattern, EXUctr_TypeEnum.Type] with DecodeAPI {
    override def name: String = "EXUctr"
    override def chiselType = EXUctr_TypeEnum()
    override def genTable(i: rvInstructionPattern): BitPat = {
        i.inst.name match {
            case "add" | "addi" | "auipc" | "jal" | "jalr" => Get_BitPat(EXUctr_TypeEnum.EXUctr_ADD)
            case "sub" | "beq" | "bne" => Get_BitPat(EXUctr_TypeEnum.EXUctr_SUB)
            case "xor" | "xori" => Get_BitPat(EXUctr_TypeEnum.EXUctr_XOR)
            case "or" | "ori" | "csrrs" => Get_BitPat(EXUctr_TypeEnum.EXUctr_OR)
            case "and" | "andi" => Get_BitPat(EXUctr_TypeEnum.EXUctr_AND)
            case "slt" | "slti" | "blt" | "bge" => Get_BitPat(EXUctr_TypeEnum.EXUctr_Less_S)
            case "sltu" | "sltui" | "bltu" | "bgeu" => Get_BitPat(EXUctr_TypeEnum.EXUctr_Less_U)
            case "sll" | "slli" => Get_BitPat(EXUctr_TypeEnum.EXUctr_SLL)
            case "srl" | "srli" => Get_BitPat(EXUctr_TypeEnum.EXUctr_SRL)
            case "sra" | "srai" => Get_BitPat(EXUctr_TypeEnum.EXUctr_SRA)
            case "csrrw" | "lui" => Get_BitPat(EXUctr_TypeEnum.EXUctr_A)
            case "lb" | "lh" | "lw" | "lbu" | "lhu"  => Get_BitPat(EXUctr_TypeEnum.EXUctr_LD)
            case "sb" | "sh" | "sw"  => Get_BitPat(EXUctr_TypeEnum.EXUctr_ST)
            case _ => BitPat.dontCare(EXUctr_TypeEnum.getWidth)
        }
    }
}

object MemOp_Field extends DecodeField[rvInstructionPattern, MemOp_TypeEnum.Type] with DecodeAPI {
    override def name: String = "memop"
    override def chiselType = MemOp_TypeEnum()
    override def genTable(i: rvInstructionPattern): BitPat = {
        i.inst.name match {
            case "lb"       => Get_BitPat(MemOp_TypeEnum.MemOp_1BS)
            case "lh"       => Get_BitPat(MemOp_TypeEnum.MemOp_2BS)
            case "lw"       => Get_BitPat(MemOp_TypeEnum.MemOp_4BU)
            case "lbu"      => Get_BitPat(MemOp_TypeEnum.MemOp_1BU)
            case "lhu"      => Get_BitPat(MemOp_TypeEnum.MemOp_2BU)
            case "sb"       => Get_BitPat(MemOp_TypeEnum.MemOp_1BS)
            case "sh"       => Get_BitPat(MemOp_TypeEnum.MemOp_2BS)
            case "sw"       => Get_BitPat(MemOp_TypeEnum.MemOp_4BU)
            case _          => BitPat.dontCare(MemOp_TypeEnum.getWidth)
        }
    }
}

object csr_ctr_Field extends DecodeField[rvInstructionPattern, CSR_TypeEnum.Type] with DecodeAPI {
    override def name: String = "csr_ctr"
    override def chiselType = CSR_TypeEnum()
    override def genTable(i: rvInstructionPattern): BitPat = {
        i.inst.name match {
            case "ecall" => Get_BitPat(CSR_TypeEnum.CSR_R1W2)
            case "mret"  => Get_BitPat(CSR_TypeEnum.CSR_R1W0)
            case _       => i.inst.args.map(_.toString).collectFirst {
                case "csr" => Get_BitPat(CSR_TypeEnum.CSR_R1W1)
            }.getOrElse(Get_BitPat(CSR_TypeEnum.CSR_N))
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

    val state = RegInit(bus_state.s_wait_valid)

    state := MuxLookup(state, bus_state.s_wait_valid)(
        Seq(
            bus_state.s_wait_valid -> Mux(io.IFU_2_IDU.valid, bus_state.s_wait_ready, bus_state.s_wait_valid),
            bus_state.s_wait_ready -> Mux(io.IDU_2_EXU.ready, bus_state.s_wait_valid, bus_state.s_wait_ready),
        )
    )

    io.IDU_2_EXU.valid := state === bus_state.s_wait_ready
    io.IFU_2_IDU.ready := state === bus_state.s_wait_valid
    val comunication_succeed = (io.IFU_2_IDU.valid && io.IFU_2_IDU.ready)

    val instTable = rvdecoderdb.fromFile.instructions(os.pwd / os.up / os.up / "src" / "ssrc" / "main" /  "rvdecoderdb" / "rvdecoderdbtest" / "jvm" / "riscv-opcodes")

    val rv32iExceptInstructions = 
        Set("sbreak", "scall", "pause", "fence.tso", "fence", "slli_rv32", "srli_rv32", "srai_rv32")
    val rviTargetSets = Set("rv_i")
    val rv32iTargetSets = Set("rv32_i")
    val rvsysTargetSets = Set("rv_system")
    val rvzicsrTargetSets = Set("rv_zicsr")

    val rviInstList = instTable
        .filter(instr => rviTargetSets.contains(instr.instructionSet.name))
        .filter(instr => !rv32iExceptInstructions.contains(instr.name))
        .filter(_.pseudoFrom.isEmpty)
        .map(rvInstructionPattern(_))
        .toSeq
    val rv32iInstList = instTable
        .filter(instr => rv32iTargetSets.contains(instr.instructionSet.name))
        .filter(instr => !rv32iExceptInstructions.contains(instr.name))
        .map(rvInstructionPattern(_))
    val rvsysInstList = instTable
        .filter(instr => rvsysTargetSets.contains(instr.instructionSet.name))
        .filter(_.pseudoFrom.isEmpty)
        .map(rvInstructionPattern(_))
    val rvzicsrInstList = instTable
        .filter(instr => rvzicsrTargetSets.contains(instr.instructionSet.name))
        .filter(_.pseudoFrom.isEmpty)
        .map(rvInstructionPattern(_))
        .toSeq
    val instList = rviInstList ++ rv32iInstList ++ rvsysInstList ++ rvzicsrInstList

    val rvdecoderTable = new DecodeTable(instList, Seq(Imm_Field, Bran_Field, EXUAsrc_Field, EXUBsrc_Field, EXUctr_Field, csr_ctr_Field, RegWr_Field, MemOp_Field))
    val rvdecoderResult = rvdecoderTable.decode(io.IFU_2_IDU.bits.data)

    val imm = MuxLookup(rvdecoderResult(Imm_Field), 0.U)(
        Seq(
            Imm_TypeEnum.Imm_I -> Cat(Fill(21, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(31, 20)),
            Imm_TypeEnum.Imm_U -> Cat(io.IFU_2_IDU.bits.data(31, 12), Fill(12, 0.U)),
            Imm_TypeEnum.Imm_S -> Cat(Fill(20, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(31, 25), io.IFU_2_IDU.bits.data(11, 7)),
            Imm_TypeEnum.Imm_B -> Cat(Fill(20, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(7), io.IFU_2_IDU.bits.data(30, 25), io.IFU_2_IDU.bits.data(11, 8), 0.U(1.W)),
            Imm_TypeEnum.Imm_J -> Cat(Fill(12, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(19, 12), io.IFU_2_IDU.bits.data(20), io.IFU_2_IDU.bits.data(30, 21), 0.U(1.W)),
        )
    )

    val csr_raddr = MuxLookup(rvdecoderResult(csr_ctr_Field), imm(11, 0))(
        Seq(
            CSR_TypeEnum.CSR_R1W0 -> "h341".U,
            CSR_TypeEnum.CSR_R1W2 -> "h305".U,
        )
    )

    val gpr_waddr = Mux(rvdecoderResult(RegWr_Field) === RegWr_TypeEnum.RegWr_Yes, io.IFU_2_IDU.bits.data(10, 7), 0.U(4.W))

    io.IDU_2_REG.CSR_raddr         <> csr_raddr

    val EXU_A = MuxLookup(rvdecoderResult(EXUAsrc_Field), 0.U)(Seq(
        EXUAsrc_TypeEnum.EXUAsrc_RS1 -> io.REG_2_IDU.GPR_Adata,
        EXUAsrc_TypeEnum.EXUAsrc_PC  -> io.REG_2_IDU.PC,
    ))

    val EXU_B = MuxLookup(rvdecoderResult(EXUBsrc_Field), 0.U)(Seq(
        EXUBsrc_TypeEnum.EXUBsrc_RS2 -> io.REG_2_IDU.GPR_Bdata,
        EXUBsrc_TypeEnum.EXUBsrc_IMM -> imm,
        EXUBsrc_TypeEnum.EXUBsrc_4   -> 4.U,
        EXUBsrc_TypeEnum.EXUBsrc_CSR -> io.REG_2_IDU.CSR_rdata,
    ))

    io.IDU_2_EXU.bits.Branch       <> RegEnable(rvdecoderResult(Bran_Field),      comunication_succeed) 
    io.IDU_2_EXU.bits.MemOp        <> RegEnable(rvdecoderResult(MemOp_Field),      comunication_succeed) 
    io.IDU_2_EXU.bits.EXU_A        <> RegEnable(EXU_A,                 comunication_succeed) 
    io.IDU_2_EXU.bits.EXU_B        <> RegEnable(EXU_B,                 comunication_succeed) 
    io.IDU_2_EXU.bits.EXUctr       <> RegEnable(rvdecoderResult(EXUctr_Field),      comunication_succeed) 
    io.IDU_2_EXU.bits.csr_ctr      <> RegEnable(rvdecoderResult(csr_ctr_Field),     comunication_succeed) 
    io.IDU_2_EXU.bits.Imm          <> RegEnable(imm,                    comunication_succeed) 
    io.IDU_2_EXU.bits.GPR_Adata    <> RegEnable(io.REG_2_IDU.GPR_Adata,  comunication_succeed) 
    io.IDU_2_EXU.bits.GPR_Bdata    <> RegEnable(io.REG_2_IDU.GPR_Bdata,  comunication_succeed) 
    io.IDU_2_EXU.bits.GPR_waddr    <> RegEnable(gpr_waddr, comunication_succeed) 
    io.IDU_2_EXU.bits.PC           <> RegEnable(io.REG_2_IDU.PC,         comunication_succeed) 
    io.IDU_2_EXU.bits.CSR_rdata    <> RegEnable(io.REG_2_IDU.CSR_rdata,  comunication_succeed) 
}
