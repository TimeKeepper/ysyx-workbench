package  BASYS

import chisel3._
import chisel3.util._

class ps2mouse extends BlackBox {
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val reset = Input(Reset())
        val ps2_clk = Input(Bool())
        val ps2_data = Input(Bool())
        val is_left_click = Output(Bool())
    })
}