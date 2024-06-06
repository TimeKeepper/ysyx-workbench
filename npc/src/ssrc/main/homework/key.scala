package homework

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

class key(val clk_Mhz: Int) extends Module {
    val io = IO(new Bundle {
        val key_in = Input(Bool())
        val is_key_posedge = Output(Bool())
    })

    val key_reg = RegInit(false.B)
    key_reg := io.key_in

    val s_idle :: s_key_wait :: Nil = Enum(2)
    val state = RegInit(s_idle)

    when(state === s_idle && key_reg === false.B && io.key_in === true.B) {
        state := s_key_wait
    }

    val delay_cnt = RegInit(0.U(32.W))
    when(state === s_key_wait) {
        delay_cnt := delay_cnt + 1.U
    }

    def delay_time = clk_Mhz * 1000000 / 1000 * 20 // 20ms

    when(delay_cnt >= delay_time.U) {
        io.is_key_posedge := true.B
        state := s_idle
        delay_cnt := 0.U
    }.otherwise {
        io.is_key_posedge := false.B
    }
}