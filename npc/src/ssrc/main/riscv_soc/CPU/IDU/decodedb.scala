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

trait DecodeAPI {
    def Get_BitPat[T <: Data](Enum: T): BitPat = {
        BitPat(Enum.litValue.U(Enum.getWidth.W))
    }
}

object my_fooldecodedb {
    val allFields = Seq(
        RegWrFiled,
        MemtoRegField,
        MemWrField,
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
