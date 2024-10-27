package  BASYS

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

class ps2mouse extends BlackBox {
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val reset = Input(Bool())
        val ps2_clk = Analog(1.W)
        val ps2_data = Analog(1.W)
        val REn = Output(Bool())
        val mouse_data = Output(UInt(24.W))
    })
}