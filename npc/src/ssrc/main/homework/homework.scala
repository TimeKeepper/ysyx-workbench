package homework

import timer._
import decoder._

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

class Homework extends Module {
    val io = IO(new Bundle{
        val sw1 = Input(Clock())
        val out = Output(UInt(8.W))
        val bit = Output(UInt(4.W))
    })

    val s_second :: s_minute :: s_10micro :: Nil = Enum(3)
    val state = RegInit(s_second)

    withClock(io.sw1) {
        state := MuxLookup(state, s_second) (Seq(
            s_second -> s_minute,
            s_minute -> s_10micro,
            s_10micro -> s_second
        ))
    }

    val total_10m_seconds = Wire(UInt(32.W))
    val total_seconds = Wire(UInt(32.W))
    val total_minutes = Wire(UInt(32.W))

    val timer = Module(new Timer(50))
    total_10m_seconds <> timer.io.time_10m_seconds
    total_seconds := timer.io.total_seconds/100.U
    total_minutes := total_seconds/60.U

    val decoder1 = Module(new decoder.BCDDecoder)
    val decoder2 = Module(new decoder.BCDDecoder)
    val decoder3 = Module(new decoder.BCDDecoder)
    val decoder4 = Module(new decoder.BCDDecoder)

    val time_type_Choice = MuxLookup(state, total_seconds) (Seq(
        s_second -> total_seconds,
        s_minute -> total_minutes,
        s_10micro -> total_10m_seconds
    ))

    decoder1.io.in := (time_type_Choice % 10.U)(3, 0)
    decoder2.io.in := (time_type_Choice / 10.U % 10.U)(3, 0)
    decoder3.io.in := (time_type_Choice / 100.U % 10.U)(3, 0)
    decoder4.io.in := (time_type_Choice / 1000.U % 10.U)(3, 0)

    val bit_reg = RegInit("b1110".U(4.W))
    val counter = RegInit(0.U(32.W))
    counter := counter + 1.U
    when(counter === 1000.U) {
        counter := 0.U
        bit_reg := Cat(bit_reg(2, 0), bit_reg(3))
    }

    io.out := MuxLookup(bit_reg, 0.U(8.W)) (Seq(
        "b1110".U -> decoder1.io.out,
        "b1101".U -> decoder2.io.out,
        "b1011".U -> decoder3.io.out,
        "b0111".U -> decoder4.io.out
    ))

    io.bit := bit_reg
}