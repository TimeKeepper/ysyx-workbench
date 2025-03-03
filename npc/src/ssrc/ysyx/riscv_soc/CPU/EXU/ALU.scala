package riscv_cpu

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

import signal_value._
import bus_state._
import config._

// riscv cpu analogic and logical unit

class ALU_catch extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle{
    val AL = Input(Bool())
  })
  setInline("ALU_catch.v",
  """module ALU_catch(
  |   input AL
  |);
  |  import "DPI-C" function void ALU_catch();
  |  always @(posedge AL) begin
  |       ALU_catch();
  |  end
  |endmodule
  """.stripMargin)
}

class ALU extends Module {
  val io = IO(new Bundle {
    val IDU_2_EXU = Flipped(Decoupled(Input(new BUS_IDU_2_EXU)))

    val out = Decoupled(new Bundle{
      val Result = Output(UInt(32.W)) 
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

  // ALU operation
  val Sub_Add = Wire(Bool())

  when(io.IDU_2_EXU.bits.EXUctr === EXUctr_TypeEnum.EXUctr_ADD) {
    Sub_Add := N
  }.otherwise {
    Sub_Add := Y
  }

  // ALU Adder
  val src_A      = io.IDU_2_EXU.bits.EXU_A
  val src_B      = io.IDU_2_EXU.bits.EXU_B

  val adder    = Wire(UInt(32.W))
  adder := src_A + Mux(Sub_Add, ~src_B, src_B) + Sub_Add

  // ALU BarrelShifter

  val shifter_result = MuxLookup(io.IDU_2_EXU.bits.EXUctr, 0.U)(Seq(
    EXUctr_TypeEnum.EXUctr_SLL -> (src_A << src_B(4, 0))(31, 0),
    EXUctr_TypeEnum.EXUctr_SRL -> (src_A >> src_B(4, 0))(31, 0),
    EXUctr_TypeEnum.EXUctr_SRA -> (src_A.asSInt >> src_B(4, 0))(31, 0)
  ))

  // other ALU outputs
  val Less = Wire(Bool())
  when(io.IDU_2_EXU.bits.EXUctr === EXUctr_TypeEnum.EXUctr_Less_U){
    Less := src_A.asUInt < src_B.asUInt
  }.otherwise{
    Less := src_A.asSInt < src_B.asSInt
  }

  val Result = MuxLookup(io.IDU_2_EXU.bits.EXUctr, 0.U)(
    Seq(
      EXUctr_TypeEnum.EXUctr_ADD      -> adder,
      EXUctr_TypeEnum.EXUctr_SUB      -> adder,
      EXUctr_TypeEnum.EXUctr_Less_U   -> Cat(0.U(31.W), Less),
      EXUctr_TypeEnum.EXUctr_Less_S   -> Cat(0.U(31.W), Less),
      EXUctr_TypeEnum.EXUctr_A        -> src_A,
      EXUctr_TypeEnum.EXUctr_B        -> src_B,
      EXUctr_TypeEnum.EXUctr_SLL      -> shifter_result,
      EXUctr_TypeEnum.EXUctr_SRL      -> shifter_result,
      EXUctr_TypeEnum.EXUctr_SRA      -> shifter_result,
      EXUctr_TypeEnum.EXUctr_XOR      -> (src_A ^ src_B),
      EXUctr_TypeEnum.EXUctr_OR       -> (src_A | src_B),
      EXUctr_TypeEnum.EXUctr_AND      -> (src_A & src_B)
    )
  )
  
  io.out.bits.Result        := Result

  if(Config.Simulate){
    val Catch = Module(new ALU_catch)
    Catch.io.AL := io.out.fire && !reset.asBool
  }
}
