package homework

import timer._
import decoder._

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

class Homework extends Module {
    val io = IO(new Bundle{
        val out = Output(Vec(4, UInt(8.W)))
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

    decoder1.io.out <> io.out(0)
    decoder2.io.out <> io.out(1)
    decoder3.io.out <> io.out(2)
    decoder4.io.out <> io.out(3)
}