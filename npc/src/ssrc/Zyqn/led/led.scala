package ssrc.Zyqn.led

import chisel3._
import chisel3.util._

class blinkIO extends Bundle {
    val led = Output(Bool())
}

class blink(Freq_clock: Int, Freq_blink: Int) extends Module {
    val io = IO(new blinkIO)

    assert(Freq_blink < Freq_clock / 2, "Blink frequency must be less than clock frequency")
    val chasig = Freq_clock / Freq_blink / 2

    val counter = RegInit(0.U(log2Ceil(chasig).W))

    counter := Mux(counter === (chasig - 1).U, 0.U, counter + 1.U)
    io.led := RegEnable(~io.led, counter === 0.U)
}

class waterIO extends Bundle {
    val led1 = Output(Bool())
    val led2 = Output(Bool())
}

class water(Freq_clock: Int, Freq_blink: Int) extends Module {
    val io = IO(new waterIO)

    assert(Freq_blink < Freq_clock / 2, "Blink frequency must be less than clock frequency")
    val chasig = Freq_clock / Freq_blink / 2

    val counter = RegInit(0.U(log2Ceil(chasig).W))

    counter := Mux(counter === (chasig - 1).U, 0.U, counter + 1.U)
    
    val water_reg = RegInit(1.U(2.W))
    when(counter === 0.U){
        water_reg := Cat(water_reg(0), water_reg(1))
    }

    io.led1 := water_reg(0)
    io.led2 := water_reg(1)
}
