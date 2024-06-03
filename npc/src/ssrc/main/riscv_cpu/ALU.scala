package riscv_cpu

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup


import signal_value._

// riscv cpu analogic and logical unit

class ALU_Ctrl extends Module {
  val io = IO(new Bundle {
    val ALUctr = Input(ALUctr_Type)

    val A_L     = Output(Bool())
    val L_R     = Output(Bool())
    val U_S     = Output(Bool())
    val Sub_Add = Output(Bool())
  })

  when(io.ALUctr === ALU_Less_U || io.ALUctr === ALU_SRL) {
    io.A_L := N
  }.otherwise {
    io.A_L := Y
  }

  when(io.ALUctr === ALU_SLL) {
    io.L_R := Y
  }.otherwise {
    io.L_R := N
  }

  when(io.ALUctr === ALU_Less_U) {
    io.U_S := Y
  }.otherwise {
    io.U_S := N
  }

  when(io.ALUctr === ALU_ADD) {
    io.Sub_Add := N
  }.otherwise {
    io.Sub_Add := Y
  }
}

class ALU_Adder extends Module {
  val io = IO(new Bundle {
    val A   = Input(UInt(32.W))
    val B   = Input(UInt(32.W))
    val Cin = Input(Bool())

    val Carry    = Output(Bool())
    val Zero     = Output(Bool())
    val Overflow = Output(Bool())
    val Result   = Output(UInt(32.W))
  })

  val R_B = Wire(UInt(32.W))
  R_B := io.B +% io.Cin

  val add_result = Wire(UInt(33.W))
  add_result := io.A +& io.B +& io.Cin

  io.Carry  := add_result(32)
  io.Result := add_result(31, 0)
  io.Zero   := io.Result === 0.U

  io.Overflow := (io.A(31) & R_B(31) & !io.Result(31)) | (!io.A(31) & !R_B(31) & io.Result(31))
}

class ALU_BarrelShifter extends Module {
  val io = IO(new Bundle {
    val Din   = Input(UInt(32.W))
    val shamt = Input(UInt(5.W))
    val L_R   = Input(Bool())
    val A_L   = Input(Bool())

    val Dout = Output(UInt(32.W))
  })

  when(io.L_R) {
    when(io.A_L) {
      io.Dout := (io.Din.asSInt << io.shamt)(31, 0)
    }.otherwise {
      io.Dout := (io.Din << io.shamt)(31, 0)
    }
  }.otherwise {
    when(io.A_L) {
      io.Dout := (io.Din.asSInt >> io.shamt)(31, 0)
    }.otherwise {
      io.Dout := (io.Din >> io.shamt)(31, 0)
    }
  }
}

class ALU extends Module {
  val io = IO(new Bundle {
    val ALUctr = Input(ALUctr_Type)
    val src_A  = Input(UInt(32.W))
    val src_B  = Input(UInt(32.W))

    val ALUout = Output(UInt(32.W))
    val Zero   = Output(Bool())
    val Less   = Output(Bool())
  })

  // ALU operation
  val alu_ctrl = Module(new ALU_Ctrl)
  alu_ctrl.io.ALUctr := io.ALUctr

  val A_L     = Wire(Bool())
  val L_R     = Wire(Bool())
  val U_S     = Wire(Bool())
  val Sub_Add = Wire(Bool())
  A_L     := alu_ctrl.io.A_L
  L_R     := alu_ctrl.io.L_R
  U_S     := alu_ctrl.io.U_S
  Sub_Add := alu_ctrl.io.Sub_Add

  // ALU Adder
  val Sub_Add_ex = Wire(SInt(32.W))
  val A1         = Wire(UInt(32.W))
  val B1         = Wire(UInt(32.W))

  Sub_Add_ex := Sub_Add.asSInt
  A1         := io.src_A
  B1         := io.src_B ^ Sub_Add_ex.asUInt

  val alu_adder = Module(new ALU_Adder)
  alu_adder.io.A   := A1
  alu_adder.io.B   := B1
  alu_adder.io.Cin := Sub_Add

  val Carry    = Wire(Bool())
  val adder    = Wire(UInt(32.W))
  val Overflow = Wire(Bool())
  val Zero     = Wire(Bool())
  Carry    := alu_adder.io.Carry
  adder    := alu_adder.io.Result
  Overflow := alu_adder.io.Overflow
  Zero     := alu_adder.io.Zero

  io.Zero := Zero

  // ALU BarrelShifter
  val alu_barrel_shifter = Module(new ALU_BarrelShifter)
  alu_barrel_shifter.io.Din   := io.src_A
  alu_barrel_shifter.io.shamt := io.src_B(4, 0)
  alu_barrel_shifter.io.L_R   := L_R
  alu_barrel_shifter.io.A_L   := A_L

  val shift = Wire(UInt(32.W))
  shift := alu_barrel_shifter.io.Dout

  // other ALU outputs
  val Less = Wire(Bool())
  when(U_S) {
    Less := Sub_Add ^ Carry
  }.elsewhen(io.src_B === "h80000000".U && Sub_Add) {
    // 数学上来说，一个负数的相反数不可能是负数，但是二进制补码可就要例外了，所以这里要特判一下
    Less := N
  }.otherwise {
    Less := adder(31) ^ Overflow
  }
  io.Less := Less

  val slt = Cat(0.U(31.W), Less)

  val B = io.src_B
  val A = io.src_A

  val XOR = Wire(UInt(32.W))
  val OR  = Wire(UInt(32.W))
  val AND = Wire(UInt(32.W))

  XOR := io.src_A ^ io.src_B
  OR  := io.src_A | io.src_B
  AND := io.src_A & io.src_B

  val Result = MuxLookup(io.ALUctr, 0.U)(Seq(
    ALU_ADD  -> adder,
    ALU_SUB  -> adder,
    ALU_Less_U -> slt,
    ALU_Less_S -> slt,
    ALU_A    -> A,
    ALU_B    -> B,
    ALU_SLL  -> shift,
    ALU_SRL  -> shift,
    ALU_SRA  -> shift,
    ALU_XOR  -> XOR,
    ALU_OR   -> OR,
    ALU_AND  -> AND,
  ))

  io.ALUout := Result
}
