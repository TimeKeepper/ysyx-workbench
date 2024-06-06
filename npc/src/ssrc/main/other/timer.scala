package timer

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

class Timer(val clk_Mhz: Int) extends Module {
    val io = IO(new Bundle {
        val time_10m_seconds = Output(UInt(32.W))
        val clear = Input(Bool())
        val stop = Input(Bool())
        val up_or_down = Input(Bool())
    })

    val timer_counter = RegInit(0.U(32.W))

    val total_10m_seconds = RegInit(0.U(32.W))

    when(!io.clear){
        timer_counter := 0.U
    }.eslewhen(io.stop){
        timer_counter := timer_counter
    }.elsewhen(io.up_or_down){
        timer_counter := timer_counter + 1.U
    }.otherwise{
        timer_counter := timer_counter - 1.U
    }

    when(timer_counter === ((clk_Mhz * 1024 * 1024)/100 - 1).U) {
        timer_counter := 0.U
        total_10m_seconds := total_10m_seconds + 1.U
    }

    io.time_10m_seconds := total_10m_seconds
}