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

    val out = Decoupled(new Bundle{
      val Result = Output(UInt(32.W)) 
      val Ano = Output(UInt(32.W))
      val is_jmp = Output(Bool())
    })
  })

  val state = RegInit(bus_state.s_wait_valid)

  state := MuxLookup(state, bus_state.s_wait_valid)(
      Seq(
          bus_state.s_wait_valid -> Mux(io.IDU_2_EXU.valid,  bus_state.s_wait_ready, bus_state.s_wait_valid),
          bus_state.s_wait_ready -> Mux(io.out.ready, bus_state.s_wait_valid, bus_state.s_wait_ready),
      )
  )

  io.out.valid := state === bus_state.s_wait_ready
  io.IDU_2_EXU.ready  := state === bus_state.s_wait_valid
  val comunication_succeed = (io.IDU_2_EXU.valid && io.IDU_2_EXU.ready)

  // ALU operation
  val Sub_Add = Wire(Bool())

  when(io.IDU_2_EXU.bits.ALUctr === ALUctr_TypeEnum.ALUctr_ADD) {
    Sub_Add := N
  }.otherwise {
    Sub_Add := Y
  }

  // ALU Adder
  val src_A      = Wire(UInt(32.W))
  val src_B      = Wire(UInt(32.W))

  src_A := MuxLookup(io.IDU_2_EXU.bits.ALUAsrc, 0.U)(Seq(
      ALUAsrc_TypeEnum.ALUAsrc_RS1 -> io.IDU_2_EXU.bits.GPR_Adata,
      ALUAsrc_TypeEnum.ALUAsrc_PC  -> io.IDU_2_EXU.bits.PC,
      ALUAsrc_TypeEnum.ALUAsrc_CSR -> io.IDU_2_EXU.bits.CSR_rdata,
  ))

  src_B := MuxLookup(io.IDU_2_EXU.bits.ALUBsrc, 0.U)(Seq(
      ALUBsrc_TypeEnum.ALUBsrc_RS1 -> io.IDU_2_EXU.bits.GPR_Adata,
      ALUBsrc_TypeEnum.ALUBsrc_RS2 -> io.IDU_2_EXU.bits.GPR_Bdata,
      ALUBsrc_TypeEnum.ALUBsrc_IMM -> io.IDU_2_EXU.bits.Imm,
      ALUBsrc_TypeEnum.ALUBsrc_4   -> 4.U,
  ))

  val adder    = Wire(UInt(32.W))
  adder := src_A + Mux(Sub_Add, ~src_B, src_B) + Sub_Add

  // ALU BarrelShifter

  val shifter_result = MuxLookup(io.IDU_2_EXU.bits.ALUctr, 0.U)(Seq(
    ALUctr_TypeEnum.ALUctr_SLL -> (src_A << src_B(4, 0))(31, 0),
    ALUctr_TypeEnum.ALUctr_SRL -> (src_A >> src_B(4, 0))(31, 0),
    ALUctr_TypeEnum.ALUctr_SRA -> (src_A.asSInt >> src_B(4, 0))(31, 0)
  ))

  // other ALU outputs
  val Less = Wire(Bool())
  when(io.IDU_2_EXU.bits.ALUctr === ALUctr_TypeEnum.ALUctr_Less_U){
    Less := src_A.asUInt < src_B.asUInt
  }.otherwise{
    Less := src_A.asSInt < src_B.asSInt
  }

  val Result = MuxLookup(io.IDU_2_EXU.bits.ALUctr, 0.U)(
    Seq(
      ALUctr_TypeEnum.ALUctr_ADD -> adder,
      ALUctr_TypeEnum.ALUctr_SUB -> adder,
      ALUctr_TypeEnum.ALUctr_Less_U -> Cat(0.U(31.W), Less),
      ALUctr_TypeEnum.ALUctr_Less_S -> Cat(0.U(31.W), Less),
      ALUctr_TypeEnum.ALUctr_A -> src_A,
      ALUctr_TypeEnum.ALUctr_B -> src_B,
      ALUctr_TypeEnum.ALUctr_SLL -> shifter_result,
      ALUctr_TypeEnum.ALUctr_SRL -> shifter_result,
      ALUctr_TypeEnum.ALUctr_SRA -> shifter_result,
      ALUctr_TypeEnum.ALUctr_XOR -> (src_A ^ src_B),
      ALUctr_TypeEnum.ALUctr_OR -> (src_A | src_B),
      ALUctr_TypeEnum.ALUctr_AND -> (src_A & src_B)
    )
  )
  
  val is_jmp = MuxLookup(io.IDU_2_EXU.bits.Branch, false.B)(Seq(
    Bran_TypeEnum.Bran_Jeq -> Mux(src_A === src_B, true.B, false.B),
    Bran_TypeEnum.Bran_Jne -> Mux(src_A =/= src_B, true.B, false.B),
    Bran_TypeEnum.Bran_Jlt -> Mux(Less, true.B, false.B),
    Bran_TypeEnum.Bran_Jge -> Mux(!Less, true.B, false.B)
  ))

  val Jmp_Pc = MuxLookup(io.IDU_2_EXU.bits.Branch, io.IDU_2_EXU.bits.PC + io.IDU_2_EXU.bits.Imm)(Seq(
      Bran_TypeEnum.Bran_Jmpr -> (io.IDU_2_EXU.bits.GPR_Adata + io.IDU_2_EXU.bits.Imm),
      Bran_TypeEnum.Bran_Jcsr -> (io.IDU_2_EXU.bits.CSR_rdata)
  ))

  val Ano = Mux(io.out.is_jmp, Jmp_Pc, io.IDU_2_EXU.bits.CSR_rdata)
  
  io.out.bits.Result        := RegEnable(Result, comunication_succeed) 
  io.out.bits.Ano           := RegEnable(Ano, comunication_succeed)
  io.out.bits.is_jmp        := RegEnable(is_jmp, comunication_succeed)

  if(Config.DPIC_on){
      val ALU_PC = Module(new ALU_PC)
      ALU_PC.io.clock := clock
      ALU_PC.io.valid := io.out.valid && io.out.ready && !reset.asBool
  }
}
