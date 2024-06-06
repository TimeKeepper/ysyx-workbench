package homework

import timer._
import decoder._

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

class Homework extends Module {
    val io = IO(new Bundle{
        val out = Output(UInt(8.W))
        val bit = Output(UInt(4.W))
    })

    val time_seconds = Wire(UInt(32.W))

    val timer = Module(new Timer(50))
    time_seconds <> timer.io.time_seconds

    val decoder1 = Module(new decoder.BCDDecoder)
    val decoder2 = Module(new decoder.BCDDecoder)
    val decoder3 = Module(new decoder.BCDDecoder)
    val decoder4 = Module(new decoder.BCDDecoder)

    decoder1.io.in := (time_seconds)(3, 0)
    decoder2.io.in := (time_seconds % 10.U)(3, 0)
    decoder3.io.in := (time_seconds % 100.U)(3, 0)
    decoder4.io.in := (time_seconds % 1000.U)(3, 0)

    val bit_reg = RegInit("b1110".U(4.W))
    bit_reg := Cat(bit_reg(2, 0), bit_reg(3))

    io.out := MuxLookup(bit_reg, 0.U(8.W)) (Seq(
        "b1110".U -> decoder1.io.out,
        "b1101".U -> decoder2.io.out,
        "b1011".U -> decoder3.io.out,
        "b0111".U -> decoder4.io.out
    ))

    io.bit := bit_reg
}