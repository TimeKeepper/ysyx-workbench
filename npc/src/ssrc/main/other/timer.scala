package timer

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

class Timer(val clk_Mhz: Int) extends Module {
    val io = IO(new Bundle {
        val time_seconds = Output(UInt(32.W))
    })

    val timer_counter = RegInit(0.U(32.W))

    val total_seconds = RegInit(0.U(32.W))

    timer_counter := timer_counter + 1.U

    when(timer_counter === (clk_Mhz * 1024 * 1024 - 1).U) {
        timer_counter := 0.U
        total_seconds := total_seconds + 1.U
    }

    io.time_seconds := total_seconds
}