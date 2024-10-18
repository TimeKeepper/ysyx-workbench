package riscv_cpu

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._

import signal_value._

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

object MemOpField extends DecodeField[InstructionPattern, UInt]{
    def name: String = "MemOp"
    def chiselType = MemOp_Type
    def genTable(op: InstructionPattern): BitPat = {
        (op.func7.rawString, op.func3.rawString, op.opcode.rawString) match {
            case (_, "000", _) => BitPat(MemOp_1BS)
            case (_, "001", _) => BitPat(MemOp_2BS)
            case (_, "010", _) => BitPat(MemOp_4BU)
            case (_, "100", _) => BitPat(MemOp_1BU)
            case (_, "101", _) => BitPat(MemOp_2BU)
            case (_, _, _)     => BitPat.dontCare(MemOp_width)
        }
    }
}

object my_fooldecodedb {
    val allFields = Seq(
        ImmField,
        RegWrFiled,
        BranchField,
        MemtoRegField,
        MemWrField,
        MemOpField,
    )

    val possiblePattern = Seq(
        // Loads
        InstructionPattern(
            func3 = BitPat("b000")
            opcode = BitPat("b0000011"), 
        ), // LB
        InstructionPattern(
            func3 = BitPat("b001")
            opcode = BitPat("b0000011"), 
        ), // LH
        InstructionPattern(
            func3 = BitPat("b010")
            opcode = BitPat("b0000011"), 
        ), // LW
        InstructionPattern(
            func3 = BitPat("b100")
            opcode = BitPat("b0000011"), 
        ), // LBU
        InstructionPattern(
            func3 = BitPat("b101")
            opcode = BitPat("b0000011"), 
        ), // LHU

        // Stores
        InstructionPattern(
            func3 = BitPat("b000")
            opcode = BitPat("b0100011"), 
        ), // SB
        InstructionPattern(
            func3 = BitPat("b001")
            opcode = BitPat("b0100011"), 
        ), // SH
        InstructionPattern(
            func3 = BitPat("b010")
            opcode = BitPat("b0100011"), 
        ), // SW
        
        // Shifts
        InstructionPattern(
            func7 = BitPat("b0000000")
            func3 = BitPat("b001")
            opcode = BitPat("b0110011"), 
        ), // SLL 
        InstructionPattern(
            func7 = BitPat("b0000000")
            func3 = BitPat("b001")
            opcode = BitPat("b0010011"), 
        ), // SLLI
        InstructionPattern(
            func7 = BitPat("b0000000")
            func3 = BitPat("b101")
            opcode = BitPat("b0110011"), 
        ), // SRL 
        InstructionPattern(
            func7 = BitPat("b0000000")
            func3 = BitPat("b101")
            opcode = BitPat("b0010011"), 
        ), // SRLI
        InstructionPattern(
            func7 = BitPat("b0100000")
            func3 = BitPat("b101")
            opcode = BitPat("b0110011"), 
        ), // SRA 
        InstructionPattern(
            func7 = BitPat("b0100000")
            func3 = BitPat("b101")
            opcode = BitPat("b0010011"), 
        ), // SRAI

        // Arithmetic
        InstructionPattern(
            func7 = BitPat("b0000000")
            func3 = BitPat("b000")
            opcode = BitPat("b0110011"), 
        ), // ADD  
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b000")
            opcode = BitPat("b0010011"), 
        ), // ADDI 
        InstructionPattern(
            func7 = BitPat("b0100000")
            func3 = BitPat("b000")
            opcode = BitPat("b0110011"), 
        ), // SUB  
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b???")
            opcode = BitPat("b0110111"), 
        ), // LUI  
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b???")
            opcode = BitPat("b0010111"), 
        ), // AUIPC
        
        // Logical
        InstructionPattern(
            func7 = BitPat("b0000000")
            func3 = BitPat("b100")
            opcode = BitPat("b0110011"), 
        ), // XOR 
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b100")
            opcode = BitPat("b0010011"), 
        ), // XORI
        InstructionPattern(
            func7 = BitPat("b0000000")
            func3 = BitPat("b110")
            opcode = BitPat("b0110011"), 
        ), // OR  
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b110")
            opcode = BitPat("b0010011"), 
        ), // ORI 
        InstructionPattern(
            func7 = BitPat("b0000000")
            func3 = BitPat("b111")
            opcode = BitPat("b0110011"), 
        ), // AND 
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b111")
            opcode = BitPat("b0010011"), 
        ), // ANDI

        // Compare
        InstructionPattern(
            func7 = BitPat("b0000000")
            func3 = BitPat("b010")
            opcode = BitPat("b0110011"), 
        ), // SLT  
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b010")
            opcode = BitPat("b0010011"), 
        ), // SLTI 
        InstructionPattern(
            func7 = BitPat("b0000000")
            func3 = BitPat("b011")
            opcode = BitPat("b0110011"), 
        ), // SLTU 
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b011")
            opcode = BitPat("b0010011"), 
        ), // SLTIU

        // Branches
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b000")
            opcode = BitPat("b1100011"), 
        ), // BEQ 
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b001")
            opcode = BitPat("b1100011"), 
        ), // BNE 
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b100")
            opcode = BitPat("b1100011"), 
        ), // BLT 
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b101")
            opcode = BitPat("b1100011"), 
        ), // BGE 
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b110")
            opcode = BitPat("b1100011"), 
        ), // BLTU
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b111")
            opcode = BitPat("b1100011"), 
        ), // BGEU

        // Jump & Link
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b???")
            opcode = BitPat("b1101111"), 
        ), // JAL 
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b000")
            opcode = BitPat("b1100111"), 
        ), // JALR

        // CSR Access
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b001")
            opcode = BitPat("b1110011"), 
        ), // CSRRW
        InstructionPattern(
            func7 = BitPat("b???????")
            func3 = BitPat("b010")
            opcode = BitPat("b1110011"), 
        ), // CSRRS

        // Change Level
        InstructionPattern(
            func7 = BitPat("b0000000")
            func3 = BitPat("b000")
            opcode = BitPat("b1110011"), 
        ), // ECALL
        InstructionPattern(
            func7 = BitPat("b0011000")
            func3 = BitPat("b000")
            opcode = BitPat("b1110011"), 
        ), // MRET
    )
}
