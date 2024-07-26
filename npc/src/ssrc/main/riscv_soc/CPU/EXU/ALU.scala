package riscv_cpu

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

import signal_value._
import bus_state._

// riscv cpu analogic and logical unit

class ysyx_23060198_ALU_Ctrl extends Module {
  val io = IO(new Bundle {
    val ALUctr = Input(ALUctr_Type)

    val A_L     = Output(Bool())
    val L_R     = Output(Bool())
    val U_S     = Output(Bool())
    val Sub_Add = Output(Bool())
  })

  when(io.ALUctr === ALUctr_Less_U || io.ALUctr === ALUctr_SRL) {
    io.A_L := N
  }.otherwise {
    io.A_L := Y
  }

  when(io.ALUctr === ALUctr_SLL) {
    io.L_R := Y
  }.otherwise {
    io.L_R := N
  }

  when(io.ALUctr === ALUctr_Less_U) {
    io.U_S := Y
  }.otherwise {
    io.U_S := N
  }

  when(io.ALUctr === ALUctr_ADD) {
    io.Sub_Add := N
  }.otherwise {
    io.Sub_Add := Y
  }
}

class ysyx_23060198_ALU_Adder extends Module {
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

class ysyx_23060198_ALU_BarrelShifter extends Module {
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

class ysyx_23060198_ALU extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new Bundle{
      val GNU_io    = Input(new GNU_Output)

      // Form Register File
      val CSR       = Input(UInt(32.W))
    }))

    val out = Decoupled(new Bundle{
      val Result = Output(UInt(32.W)) 
      val Zero   = Output(Bool())   
      val Less   = Output(Bool())
    })
  })

  val state = RegInit(s_wait_valid)

  state := MuxLookup(state, s_wait_valid)(
      Seq(
          s_wait_valid -> Mux(io.in.valid,  s_wait_ready, s_wait_valid),
          s_wait_ready -> Mux(io.out.ready, s_wait_valid, s_wait_ready),
      )
  )

  io.out.valid := state === s_wait_ready
  io.in.ready  := state === s_wait_valid
  val comunication_succeed = (io.in.valid && io.in.ready)

  // ALU operation
  val alu_ctrl = Module(new ysyx_23060198_ALU_Ctrl)
  alu_ctrl.io.ALUctr := io.in.bits.GNU_io.ALUctr

  // ALU Adder
  val Sub_Add_ex = Wire(SInt(32.W))
  val src_A      = Wire(UInt(32.W))
  val src_B      = Wire(UInt(32.W))

  src_A := MuxLookup(io.in.bits.GNU_io.ALUAsrc, 0.U)(Seq(
      ALUAsrc_RS1 -> io.in.bits.GNU_io.GPR_Adata,
      ALUAsrc_PC  -> io.in.bits.GNU_io.PC,
      ALUAsrc_CSR -> io.in.bits.CSR,
  ))

  src_B := MuxLookup(io.in.bits.GNU_io.ALUBsrc, 0.U)(Seq(
      ALUBSrc_RS1 -> io.in.bits.GNU_io.GPR_Adata,
      ALUBSrc_RS2 -> io.in.bits.GNU_io.GPR_Bdata,
      ALUBSrc_IMM -> io.in.bits.GNU_io.Imm,
      ALUBSrc_4   -> 4.U,
  ))

  Sub_Add_ex := alu_ctrl.io.Sub_Add.asSInt

  val alu_adder = Module(new ysyx_23060198_ALU_Adder)
  alu_adder.io.A   := src_A
  alu_adder.io.B   := src_B ^ Sub_Add_ex.asUInt
  alu_adder.io.Cin := alu_ctrl.io.Sub_Add

  val Carry    = Wire(Bool())
  val adder    = Wire(UInt(32.W))
  val Overflow = Wire(Bool())
  val Zero     = Wire(Bool())
  Carry    := alu_adder.io.Carry
  adder    := alu_adder.io.Result
  Overflow := alu_adder.io.Overflow
  Zero     := alu_adder.io.Zero

  // ALU BarrelShifter
  val alu_barrel_shifter = Module(new ysyx_23060198_ALU_BarrelShifter)
  alu_barrel_shifter.io.Din   := src_A
  alu_barrel_shifter.io.shamt := src_B(4, 0)
  alu_barrel_shifter.io.L_R   := alu_ctrl.io.L_R
  alu_barrel_shifter.io.A_L   := alu_ctrl.io.A_L

  // other ALU outputs
  val Less = Wire(Bool())
  when(alu_ctrl.io.U_S) {
    Less := alu_ctrl.io.Sub_Add ^ Carry
  }.elsewhen(src_B === "h80000000".U && alu_ctrl.io.Sub_Add) {
    // 数学上来说，一个负数的相反数不可能是负数，但是二进制补码可就要例外了，所以这里要特判一下
    Less := N
  }.otherwise {
    Less := adder(31) ^ Overflow
  }

  val Result = MuxLookup(io.in.bits.GNU_io.ALUctr, 0.U)(
    Seq(
      ALUctr_ADD -> adder,
      ALUctr_SUB -> adder,
      ALUctr_Less_U -> Cat(0.U(31.W), Less),
      ALUctr_Less_S -> Cat(0.U(31.W), Less),
      ALUctr_A -> src_A,
      ALUctr_B -> src_B,
      ALUctr_SLL -> alu_barrel_shifter.io.Dout,
      ALUctr_SRL -> alu_barrel_shifter.io.Dout,
      ALUctr_SRA -> alu_barrel_shifter.io.Dout,
      ALUctr_XOR -> (src_A ^ src_B),
      ALUctr_OR -> (src_A | src_B),
      ALUctr_AND -> (src_A & src_B)
    )
  )
  
  io.out.bits.Result        := RegEnable(Result, comunication_succeed) 
  io.out.bits.Zero          := RegEnable(alu_adder.io.Zero , comunication_succeed) 
  io.out.bits.Less          := RegEnable(Less, comunication_succeed) 
}
