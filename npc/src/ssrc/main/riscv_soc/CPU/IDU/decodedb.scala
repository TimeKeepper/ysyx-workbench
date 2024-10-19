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

object ImmField extends DecodeField[InstructionPattern, Imm_TypeEnum.Type] {
    def name: String = "imm"
    def chiselType = Imm_TypeEnum()
    def genTable(op: InstructionPattern): BitPat = {
        op.opcode.rawString match {
            case "0000011" => BitPat(Imm_TypeEnum.Imm_I.litValue.U(Imm_TypeEnum.getWidth.W)) // Loadxx
            case "0100011" => BitPat(Imm_TypeEnum.Imm_S.litValue.U(Imm_TypeEnum.getWidth.W)) // Storexx
            case "0010011" => BitPat(Imm_TypeEnum.Imm_I.litValue.U(Imm_TypeEnum.getWidth.W)) // xxI
            case "0110111" => BitPat(Imm_TypeEnum.Imm_U.litValue.U(Imm_TypeEnum.getWidth.W)) // LUI
            case "0010111" => BitPat(Imm_TypeEnum.Imm_U.litValue.U(Imm_TypeEnum.getWidth.W)) // AUIPC
            case "1100011" => BitPat(Imm_TypeEnum.Imm_B.litValue.U(Imm_TypeEnum.getWidth.W)) // Branchxx
            case "1101111" => BitPat(Imm_TypeEnum.Imm_J.litValue.U(Imm_TypeEnum.getWidth.W)) // JAL
            case "1100111" => BitPat(Imm_TypeEnum.Imm_I.litValue.U(Imm_TypeEnum.getWidth.W)) // JALR
            case "1110011" => BitPat(Imm_TypeEnum.Imm_I.litValue.U(Imm_TypeEnum.getWidth.W)) // CSRRx
            case _ => BitPat.dontCare(Imm_TypeEnum.getWidth)
        }
    }
}

object BranchField extends DecodeField[InstructionPattern, Bran_TypeEnum.Type] {
    def name: String = "Branch"
    def chiselType = Bran_TypeEnum()
    def genTable(op: InstructionPattern): BitPat = {
        (op.func3.rawString, op.opcode.rawString) match {
            case ("000", "1100011") => BitPat(Bran_TypeEnum.Bran_Jeq.litValue.U(Bran_TypeEnum.getWidth.W))
            case ("001", "1100011") => BitPat(Bran_TypeEnum.Bran_Jne.litValue.U(Bran_TypeEnum.getWidth.W))
            case ("100", "1100011") => BitPat(Bran_TypeEnum.Bran_Jlt.litValue.U(Bran_TypeEnum.getWidth.W))
            case ("101", "1100011") => BitPat(Bran_TypeEnum.Bran_Jge.litValue.U(Bran_TypeEnum.getWidth.W))
            case ("110", "1100011") => BitPat(Bran_TypeEnum.Bran_Jlt.litValue.U(Bran_TypeEnum.getWidth.W))
            case ("111", "1100011") => BitPat(Bran_TypeEnum.Bran_Jge.litValue.U(Bran_TypeEnum.getWidth.W))
            case ("???", "1101111") => BitPat(Bran_TypeEnum.Bran_Jmp.litValue.U(Bran_TypeEnum.getWidth.W))
            case ("000", "1100111") => BitPat(Bran_TypeEnum.Bran_Jmpr.litValue.U(Bran_TypeEnum.getWidth.W))
            case ("000", "1110011") => BitPat(Bran_TypeEnum.Bran_Jcsr.litValue.U(Bran_TypeEnum.getWidth.W))
            case (_, _) => BitPat(Bran_TypeEnum.Bran_NJmp.litValue.U(Bran_TypeEnum.getWidth.W))
        }
    }
}

object MemOpField extends DecodeField[InstructionPattern, MemOp_TypeEnum.Type]{
    def name: String = "MemOp"
    def chiselType = MemOp_TypeEnum()
    def genTable(op: InstructionPattern): BitPat = {
        (op.func7.rawString, op.func3.rawString, op.opcode.rawString) match {
            case (_, "000", _) => BitPat(MemOp_TypeEnum.MemOp_1BS.litValue.U(MemOp_TypeEnum.getWidth.W))
            case (_, "001", _) => BitPat(MemOp_TypeEnum.MemOp_2BS.litValue.U(MemOp_TypeEnum.getWidth.W))
            case (_, "010", _) => BitPat(MemOp_TypeEnum.MemOp_4BU.litValue.U(MemOp_TypeEnum.getWidth.W))
            case (_, "100", _) => BitPat(MemOp_TypeEnum.MemOp_1BU.litValue.U(MemOp_TypeEnum.getWidth.W))
            case (_, "101", _) => BitPat(MemOp_TypeEnum.MemOp_2BU.litValue.U(MemOp_TypeEnum.getWidth.W))
            case (_, _, _)     => BitPat.dontCare(MemOp_TypeEnum.getWidth)
        }
    }
}

object ALUAsrcField extends DecodeField[InstructionPattern, ALUAsrc_TypeEnum.Type] {
    def name: String = "ALUAsrc"
    def chiselType = ALUAsrc_TypeEnum()
    def genTable(op: InstructionPattern): BitPat = {
        (op.func7.rawString, op.func3.rawString, op.opcode.rawString) match {
            case (_, _, "0110011")      => BitPat(ALUAsrc_TypeEnum.ALUAsrc_RS1.litValue.U(ALUAsrc_TypeEnum.getWidth.W)) // logical
            case (_, _, "0010011")      => BitPat(ALUAsrc_TypeEnum.ALUAsrc_RS1.litValue.U(ALUAsrc_TypeEnum.getWidth.W)) // xxI
            case (_, _, "1100011")      => BitPat(ALUAsrc_TypeEnum.ALUAsrc_RS1.litValue.U(ALUAsrc_TypeEnum.getWidth.W)) // Branch
            case (_, _, "0010111")      => BitPat(ALUAsrc_TypeEnum.ALUAsrc_PC.litValue.U(ALUAsrc_TypeEnum.getWidth.W))  // AUIPC
            case (_, _, "1101111")      => BitPat(ALUAsrc_TypeEnum.ALUAsrc_PC.litValue.U(ALUAsrc_TypeEnum.getWidth.W))  // JAL
            case (_, "000", "1100111")  => BitPat(ALUAsrc_TypeEnum.ALUAsrc_PC.litValue.U(ALUAsrc_TypeEnum.getWidth.W)) // JALR
            case (_, _, "1110011")      => BitPat(ALUAsrc_TypeEnum.ALUAsrc_CSR.litValue.U(ALUAsrc_TypeEnum.getWidth.W))
            case (_, _, _) => BitPat.dontCare(ALUAsrc_TypeEnum.getWidth)
        }
    }
}

object ALUBsrcField extends DecodeField[InstructionPattern, ALUBsrc_TypeEnum.Type] {
    def name: String = "ALUBsrc"
    def chiselType = ALUBsrc_TypeEnum()
    def genTable(op: InstructionPattern): BitPat = {
        (op.func7.rawString, op.func3.rawString, op.opcode.rawString) match {
            case (_, _, "0110011")      => BitPat(ALUBsrc_TypeEnum.ALUBsrc_RS2.litValue.U(ALUBsrc_TypeEnum.getWidth.W)) // logical
            case (_, _, "0010011")      => BitPat(ALUBsrc_TypeEnum.ALUBsrc_IMM.litValue.U(ALUBsrc_TypeEnum.getWidth.W)) // xxI
            case (_, _, "1100011")      => BitPat(ALUBsrc_TypeEnum.ALUBsrc_RS2.litValue.U(ALUBsrc_TypeEnum.getWidth.W)) // Branch
            case (_, _, "0010111")      => BitPat(ALUBsrc_TypeEnum.ALUBsrc_IMM.litValue.U(ALUBsrc_TypeEnum.getWidth.W))  // AUIPC
            case (_, _, "1101111")      => BitPat(ALUBsrc_TypeEnum.ALUBsrc_4.litValue.U(ALUBsrc_TypeEnum.getWidth.W))  // JAL
            case (_, "000", "1100111")  => BitPat(ALUBsrc_TypeEnum.ALUBsrc_4.litValue.U(ALUBsrc_TypeEnum.getWidth.W)) // JALR
            case (_, _, "0110111")      => BitPat(ALUBsrc_TypeEnum.ALUBsrc_IMM.litValue.U(ALUBsrc_TypeEnum.getWidth.W)) // LUI
            case (_, _, "1110011")      => BitPat(ALUBsrc_TypeEnum.ALUBsrc_RS1.litValue.U(ALUBsrc_TypeEnum.getWidth.W)) // CSRRx
            case (_, _, _) => BitPat.dontCare(ALUBsrc_TypeEnum.getWidth)
        }
    }
}

object ALUctrField extends DecodeField[InstructionPattern, ALUctr_TypeEnum.Type] {
    def name: String = "ALUctr"
    def chiselType = ALUctr_TypeEnum()
    def genTable(op: InstructionPattern): BitPat = {
        (op.func7.rawString, op.func3.rawString, op.opcode.rawString) match {
            case ("0000000", "000", "0110011")  => BitPat(ALUctr_TypeEnum.ALUctr_ADD.litValue(ALUctr_TypeEnum.getWidth.W)) // ADD
            case ("0100000", "000", "0110011")  => BitPat(ALUctr_TypeEnum.ALUctr_SUB.litValue(ALUctr_TypeEnum.getWidth.W)) // SUB
            case ("0000000", "100", "0110011")  => BitPat(ALUctr_TypeEnum.ALUctr_XOR.litValue(ALUctr_TypeEnum.getWidth.W)) // XOR
            case ("0000000", "110", "0110011")  => BitPat(ALUctr_TypeEnum.ALUctr_OR.litValue(ALUctr_TypeEnum.getWidth.W)) // OR
            case ("0000000", "111", "0110011")  => BitPat(ALUctr_TypeEnum.ALUctr_AND.litValue(ALUctr_TypeEnum.getWidth.W)) // AND
            case ("0000000", "010", "0110011")  => BitPat(ALUctr_TypeEnum.ALUctr_Less_S.litValue(ALUctr_TypeEnum.getWidth.W)) // SLT
            case ("0000000", "011", "0110011")  => BitPat(ALUctr_TypeEnum.ALUctr_Less_U.litValue(ALUctr_TypeEnum.getWidth.W)) // SLTU
            case ("0000000", "001", "0110011")  => BitPat(ALUctr_TypeEnum.ALUctr_SLL.litValue(ALUctr_TypeEnum.getWidth.W)) // SLL
            case ("0000000", "101", "0110011")  => BitPat(ALUctr_TypeEnum.ALUctr_SRL.litValue(ALUctr_TypeEnum.getWidth.W)) // SRL
            case ("0100000", "101", "0110011")  => BitPat(ALUctr_TypeEnum.ALUctr_SRA.litValue(ALUctr_TypeEnum.getWidth.W)) // SRA // logical

            case (_, "000", "0010011")          => BitPat(ALUctr_TypeEnum.ALUctr_ADD.litValue(ALUctr_TypeEnum.getWidth.W)) // ADDI
            case (_, "100", "0010011")          => BitPat(ALUctr_TypeEnum.ALUctr_XOR.litValue(ALUctr_TypeEnum.getWidth.W)) // XORI
            case (_, "110", "0010011")          => BitPat(ALUctr_TypeEnum.ALUctr_OR.litValue(ALUctr_TypeEnum.getWidth.W)) // ORI
            case (_, "111", "0010011")          => BitPat(ALUctr_TypeEnum.ALUctr_AND.litValue(ALUctr_TypeEnum.getWidth.W)) // ANDI
            case (_, "010", "0010011")          => BitPat(ALUctr_TypeEnum.ALUctr_Less_S.litValue(ALUctr_TypeEnum.getWidth.W)) // SLTI
            case (_, "011", "0010011")          => BitPat(ALUctr_TypeEnum.ALUctr_Less_U.litValue(ALUctr_TypeEnum.getWidth.W)) // SLTUI
            case ("0000000", "001", "0010011")  => BitPat(ALUctr_TypeEnum.ALUctr_SLL.litValue(ALUctr_TypeEnum.getWidth.W)) // SLLI
            case ("0000000", "101", "0010011")  => BitPat(ALUctr_TypeEnum.ALUctr_SRL.litValue(ALUctr_TypeEnum.getWidth.W)) // SRLI
            case ("0100000", "101", "0010011")  => BitPat(ALUctr_TypeEnum.ALUctr_SRA.litValue(ALUctr_TypeEnum.getWidth.W)) // SRAI // logical I

            case (_, "000", "1100011")          => BitPat(ALUctr_TypeEnum.ALUctr_SUB.litValue(ALUctr_TypeEnum.getWidth.W))    // BEQ 
            case (_, "001", "1100011")          => BitPat(ALUctr_TypeEnum.ALUctr_SUB.litValue(ALUctr_TypeEnum.getWidth.W))    // BNE 
            case (_, "100", "1100011")          => BitPat(ALUctr_TypeEnum.ALUctr_Less_S.litValue(ALUctr_TypeEnum.getWidth.W)) // BLT 
            case (_, "101", "1100011")          => BitPat(ALUctr_TypeEnum.ALUctr_Less_S.litValue(ALUctr_TypeEnum.getWidth.W)) // BGE 
            case (_, "110", "1100011")          => BitPat(ALUctr_TypeEnum.ALUctr_Less_U.litValue(ALUctr_TypeEnum.getWidth.W)) // BLTU
            case (_, "111", "1100011")          => BitPat(ALUctr_TypeEnum.ALUctr_Less_U.litValue(ALUctr_TypeEnum.getWidth.W)) // BGEU // Branchj

            case (_, "001", "1110011")          => BitPat(ALUctr_TypeEnum.ALUctr_B.litValue(ALUctr_TypeEnum.getWidth.W)) // CSRRW
            case (_, "010", "1110011")          => BitPat(ALUctr_TypeEnum.ALUctr_OR.litValue(ALUctr_TypeEnum.getWidth.W)) // CSRRS // CSRRx

            case (_, _, "0010111")              => BitPat(ALUctr_TypeEnum.ALUctr_ADD.litValue(ALUctr_TypeEnum.getWidth.W))  // AUIPC
            case (_, _, "1101111")              => BitPat(ALUctr_TypeEnum.ALUctr_ADD.litValue(ALUctr_TypeEnum.getWidth.W))  // JAL
            case (_, "000", "1100111")          => BitPat(ALUctr_TypeEnum.ALUctr_ADD.litValue(ALUctr_TypeEnum.getWidth.W)) // JALR
            case (_, _, "0110111")              => BitPat(ALUctr_TypeEnum.ALUctr_B.litValue(ALUctr_TypeEnum.getWidth.W)) // LUI

            case (_, _, _) => BitPat.dontCare(ALUctr_TypeEnum.getWidth)
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

object csr_ctrField extends DecodeField[InstructionPattern, UInt] {
    def name: String = "csr_ctr"
    def chiselType = CSR_Type
    def genTable(op: InstructionPattern): BitPat = {
        (op.func7.rawString, op.func3.rawString, op.opcode.rawString) match {
            case (_, "001", "1110011") => BitPat(CSR_R1W1) // CSRRW
            case (_, "010", "1110011") => BitPat(CSR_R1W1) // CSRRS
            case ("0000000", "000", "1110011") => BitPat(CSR_R1W2) // ECALL
            case ("0011000", "000", "1110011") => BitPat(CSR_R1W0) // MRET // CSRRx
            case (_, _, _) => BitPat(CSR_N)
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
        ALUAsrcField,
        ALUBsrcField,
        ALUctrField,
        csr_ctrField,
    )

    val possiblePattern = Seq(
        // Loads
        InstructionPattern(
            func3  = BitPat("b000"    ),
            opcode = BitPat("b0000011"), 
        ), // LB
        InstructionPattern(
            func3  = BitPat("b001"    ),
            opcode = BitPat("b0000011"), 
        ), // LH
        InstructionPattern(
            func3  = BitPat("b010"    ),
            opcode = BitPat("b0000011"), 
        ), // LW
        InstructionPattern(
            func3  = BitPat("b100"    ),
            opcode = BitPat("b0000011"), 
        ), // LBU
        InstructionPattern(
            func3  = BitPat("b101"    ),
            opcode = BitPat("b0000011"), 
        ), // LHU

        // Stores
        InstructionPattern(
            func3  = BitPat("b000"    ),
            opcode = BitPat("b0100011"), 
        ), // SB
        InstructionPattern(
            func3  = BitPat("b001"    ),
            opcode = BitPat("b0100011"), 
        ), // SH
        InstructionPattern(
            func3  = BitPat("b010"    ),
            opcode = BitPat("b0100011"), 
        ), // SW
        
        // Shifts
        InstructionPattern(
            func7  = BitPat("b0000000"),
            func3  = BitPat("b001"    ),
            opcode = BitPat("b0110011"), 
        ), // SLL 
        InstructionPattern(
            func7  = BitPat("b0000000"),
            func3  = BitPat("b001"    ),
            opcode = BitPat("b0010011"), 
        ), // SLLI
        InstructionPattern(
            func7  = BitPat("b0000000"),
            func3  = BitPat("b101"    ),
            opcode = BitPat("b0110011"), 
        ), // SRL 
        InstructionPattern(
            func7  = BitPat("b0000000"),
            func3  = BitPat("b101"    ),
            opcode = BitPat("b0010011"), 
        ), // SRLI
        InstructionPattern(
            func7  = BitPat("b0100000"),
            func3  = BitPat("b101"    ),
            opcode = BitPat("b0110011"), 
        ), // SRA 
        InstructionPattern(
            func7  = BitPat("b0100000"),
            func3  = BitPat("b101"    ),
            opcode = BitPat("b0010011"), 
        ), // SRAI

        // Arithmetic
        InstructionPattern(
            func7  = BitPat("b0000000"),
            func3  = BitPat("b000"    ),
            opcode = BitPat("b0110011"), 
        ), // ADD  
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b000"    ),
            opcode = BitPat("b0010011"), 
        ), // ADDI 
        InstructionPattern(
            func7  = BitPat("b0100000"),
            func3  = BitPat("b000"    ),
            opcode = BitPat("b0110011"), 
        ), // SUB  
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b???"    ),
            opcode = BitPat("b0110111"), 
        ), // LUI  
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b???"    ),
            opcode = BitPat("b0010111"), 
        ), // AUIPC
        
        // Logical
        InstructionPattern(
            func7  = BitPat("b0000000"),
            func3  = BitPat("b100"    ),
            opcode = BitPat("b0110011"), 
        ), // XOR 
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b100"    ),
            opcode = BitPat("b0010011"), 
        ), // XORI
        InstructionPattern(
            func7  = BitPat("b0000000"),
            func3  = BitPat("b110"    ),
            opcode = BitPat("b0110011"), 
        ), // OR  
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b110"    ),
            opcode = BitPat("b0010011"), 
        ), // ORI 
        InstructionPattern(
            func7  = BitPat("b0000000"),
            func3  = BitPat("b111"    ),
            opcode = BitPat("b0110011"), 
        ), // AND 
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b111"    ),
            opcode = BitPat("b0010011"), 
        ), // ANDI

        // Compare
        InstructionPattern(
            func7  = BitPat("b0000000"),
            func3  = BitPat("b010"    ),
            opcode = BitPat("b0110011"), 
        ), // SLT  
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b010"    ),
            opcode = BitPat("b0010011"), 
        ), // SLTI 
        InstructionPattern(
            func7  = BitPat("b0000000"),
            func3  = BitPat("b011"    ),
            opcode = BitPat("b0110011"), 
        ), // SLTU 
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b011"    ),
            opcode = BitPat("b0010011"), 
        ), // SLTIU

        // Branches
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b000"    ),
            opcode = BitPat("b1100011"), 
        ), // BEQ 
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b001"    ),
            opcode = BitPat("b1100011"), 
        ), // BNE 
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b100"    ),
            opcode = BitPat("b1100011"), 
        ), // BLT 
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b101"    ),
            opcode = BitPat("b1100011"), 
        ), // BGE 
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b110"    ),
            opcode = BitPat("b1100011"), 
        ), // BLTU
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b111"    ),
            opcode = BitPat("b1100011"), 
        ), // BGEU

        // Jump & Link
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b???"    ),
            opcode = BitPat("b1101111"), 
        ), // JAL 
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b000"    ),
            opcode = BitPat("b1100111"), 
        ), // JALR

        // CSR Access
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b001"    ),
            opcode = BitPat("b1110011"), 
        ), // CSRRW
        InstructionPattern(
            func7  = BitPat("b???????"),
            func3  = BitPat("b010"    ),
            opcode = BitPat("b1110011"), 
        ), // CSRRS

        // Change Level
        InstructionPattern(
            func7  = BitPat("b0000000"),
            func3  = BitPat("b000"    ),
            opcode = BitPat("b1110011"), 
        ), // ECALL
        InstructionPattern(
            func7  = BitPat("b0011000"),
            func3  = BitPat("b000"    ),
            opcode = BitPat("b1110011"), 
        ), // MRET
    )
}
