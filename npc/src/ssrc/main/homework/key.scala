package homework

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

class key extends Module {
    val io = IO(new Bundle {
        val key_in = Input(Bool())
        val is_key_posedge = Output(Bool())
    })

    val key_in_filited = Wire(Bool())
    val key_in_debouncer = Module(new Debouncer(100))

    key_in_debouncer.io.input := io.key_in
    key_in_filited := key_in_debouncer.io.output

    val key_in_pre = RegInit(false.B)

    key_in_pre := key_in_filited

    when(key_in_pre === false.B && key_in_filited === true.B) {
        io.is_key_posedge := true.B
    }.otherwise {
        io.is_key_posedge := false.B
    }


    // val key_reg = RegInit(true.B)
    // key_reg := io.key_in

    // val s_idle :: s_key_wait :: Nil = Enum(2)
    // val state = RegInit(s_idle)

    // when(state === s_idle && key_reg === false.B && io.key_in === true.B) {
    //     state := s_key_wait
    // }

    // val delay_cnt = RegInit(0.U(64.W))
    // when(state === s_key_wait) {
    //     delay_cnt := delay_cnt + 1.U
    // }

    // def delay_time = clk_Mhz * 1000000 / 1000 * 20 // 20ms

    // when(delay_cnt >= delay_time.U) {
    //     io.is_key_posedge := true.B
    //     state := s_idle
    //     delay_cnt := 0.U
    // }.otherwise {
    //     io.is_key_posedge := false.B
    // }
}