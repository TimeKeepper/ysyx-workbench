package riscv_cpu

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

import signal_value._
import bus_state._
import config._

// riscv cpu analogic and logical unit

class ALU_PC extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val valid = Input(Bool())
    })
    setInline("ALU_PC.v",
    """module ALU_PC(
    |    input clock,
    |    input valid
    |);
    |  import "DPI-C" function void ALU_finished();
    |  always @(posedge clock) begin
    |    if(valid) begin
    |      ALU_finished();
    |    end
    |  end
    |endmodule
    """.stripMargin)
}

class ysyx_23060198_ALU extends Module {
  val io = IO(new Bundle {
    val IDU_2_EXU = Flipped(Decoupled(Input(new BUS_IDU_2_EXU)))
    val CSR       = Input(UInt(32.W))

    val out = Decoupled(new Bundle{
      val Result = Output(UInt(32.W)) 
      val Zero   = Output(Bool())   
      val Less   = Output(Bool())
    })
  })

  val state = RegInit(s_wait_valid)

  state := MuxLookup(state, s_wait_valid)(
      Seq(
          s_wait_valid -> Mux(io.IDU_2_EXU.valid,  s_wait_ready, s_wait_valid),
          s_wait_ready -> Mux(io.out.ready, s_wait_valid, s_wait_ready),
      )
  )

  io.out.valid := state === s_wait_ready
  io.IDU_2_EXU.ready  := state === s_wait_valid
  val comunication_succeed = (io.IDU_2_EXU.valid && io.IDU_2_EXU.ready)

  // ALU operation
  val U_S     = Wire(Bool())
  val Sub_Add = Wire(Bool())

  when(io.IDU_2_EXU.bits.ALUctr === ALUctr_Less_U) {
    U_S := Y
  }.otherwise {
    U_S := N
  }

  when(io.IDU_2_EXU.bits.ALUctr === ALUctr_ADD) {
    Sub_Add := N
  }.otherwise {
    Sub_Add := Y
  }

  // ALU Adder
  val Sub_Add_ex = Wire(SInt(32.W))
  val src_A      = Wire(UInt(32.W))
  val src_B      = Wire(UInt(32.W))

  src_A := MuxLookup(io.IDU_2_EXU.bits.ALUAsrc, 0.U)(Seq(
      ALUAsrc_RS1 -> io.IDU_2_EXU.bits.GPR_Adata,
      ALUAsrc_PC  -> io.IDU_2_EXU.bits.PC,
      ALUAsrc_CSR -> io.CSR,
  ))

  src_B := MuxLookup(io.IDU_2_EXU.bits.ALUBsrc, 0.U)(Seq(
      ALUBSrc_RS1 -> io.IDU_2_EXU.bits.GPR_Adata,
      ALUBSrc_RS2 -> io.IDU_2_EXU.bits.GPR_Bdata,
      ALUBSrc_IMM -> io.IDU_2_EXU.bits.Imm,
      ALUBSrc_4   -> 4.U,
  ))

  Sub_Add_ex := Sub_Add.asSInt

  val add_result = Wire(UInt(33.W))
  add_result := src_A +& (src_B ^ Sub_Add_ex.asUInt) +& Sub_Add

  val R_B = Wire(UInt(32.W))
  R_B := (src_B ^ Sub_Add_ex.asUInt) +% Sub_Add

  val Carry    = Wire(Bool())
  val adder    = Wire(UInt(32.W))
  val Overflow = Wire(Bool())
  val Zero     = Wire(Bool())
  Carry    := add_result(32)
  adder    := add_result(31, 0)
  Overflow := (src_A(31) & R_B(31) & !adder(31)) | (!src_A(31) & !R_B(31) & adder(31))
  Zero     := adder === 0.U

  // ALU BarrelShifter

  val shifter_result = MuxLookup(io.IDU_2_EXU.bits.ALUctr, 0.U)(Seq(
    ALUctr_SLL -> (src_A << src_B(4, 0))(31, 0),
    ALUctr_SRL -> (src_A >> src_B(4, 0))(31, 0),
    ALUctr_SRA -> (src_A.asSInt >> src_B(4, 0))(31, 0)
  ))

  // other ALU outputs
  val Less = Wire(Bool())
  when(U_S) {
    Less := Sub_Add ^ Carry
  }.elsewhen(src_B === "h80000000".U && Sub_Add) {
    // 数学上来说，一个负数的相反数不可能是负数，但是二进制补码可就要例外了，所以这里要特判一下
    Less := N
  }.otherwise {
    Less := adder(31) ^ Overflow
  }

  val Result = MuxLookup(io.IDU_2_EXU.bits.ALUctr, 0.U)(
    Seq(
      ALUctr_ADD -> adder,
      ALUctr_SUB -> adder,
      ALUctr_Less_U -> Cat(0.U(31.W), Less),
      ALUctr_Less_S -> Cat(0.U(31.W), Less),
      ALUctr_A -> src_A,
      ALUctr_B -> src_B,
      ALUctr_SLL -> shifter_result,
      ALUctr_SRL -> shifter_result,
      ALUctr_SRA -> shifter_result,
      ALUctr_XOR -> (src_A ^ src_B),
      ALUctr_OR -> (src_A | src_B),
      ALUctr_AND -> (src_A & src_B)
    )
  )
  
  io.out.bits.Result        := RegEnable(Result, comunication_succeed) 
  io.out.bits.Zero          := RegEnable(Zero , comunication_succeed) 
  io.out.bits.Less          := RegEnable(Less, comunication_succeed) 

  if(Config.DPIC_on){
      val ALU_PC = Module(new ALU_PC)
      ALU_PC.io.clock := clock
      ALU_PC.io.valid := io.out.valid && io.out.ready && !reset.asBool
  }
}
