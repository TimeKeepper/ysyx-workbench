package  homework

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

class PS2Receiver extends Module {
    val io = IO(new Bundle {
        val kclk = Input(Clock())
        val kdata = Input(Bool())
        val keycode = Decoupled(UInt(8.W))
    })

    val kclk_f = Wire(Clock())
    val kdata_f = Wire(UInt(1.W))

    val kclk_filiter = Module(new Debouncer(20))
    val kdata_filiter = Module(new Debouncer(20))

    kclk_filiter.io.input := io.kclk.asUInt
    kclk_f := (!kclk_filiter.io.output).asClock

    kdata_filiter.io.input := io.kdata
    kdata_f := kdata_filiter.io.output

    val data_cur = RegInit(0.U(8.W))

    val flag_prev = RegInit(false.B)
    val flag_cur = Wire(Bool())
    flag_prev := flag_cur

    val cnt = RegInit(0.U(4.W))

    withClock(kclk_f) {
        when(cnt === 11.U){
            cnt := 0.U
        }.otherwise{
            cnt := cnt + 1.U
        }

        data_cur := Cat(data_cur(6,0), kdata_f)

        when(cnt === 10.U) {
            flag_cur := true.B
            io.keycode.bits := data_cur
        }.otherwise {
            flag_cur := false.B
        }
    }

    io.keycode.valid := flag_prev === false.B && flag_cur === true.B
}

class Mouse_Ps2_Controller extends Module {
    val io = IO(new Bundle{
        val kclk = Input(Clock())
        val kdata = Input(Bool())
        val mouse_left_click = Output(Bool())
    })

    val ps2_receiver = Module(new PS2Receiver)
    ps2_receiver.io.kclk := io.kclk
    ps2_receiver.io.kdata := io.kdata

    val keynode = Wire(Decoupled(UInt(8.W)))
    ps2_receiver.io.keycode <> keynode

    val s_idle :: s_receiving :: Nil = Enum(2)

    val state = RegInit(s_idle)

    val cnt = RegInit(0.U(8.W))
    switch(state) {
        is(s_idle) {
            when(keynode.valid) {
                cnt := cnt + 1.U
            }
            when(cnt === 3.U){
                cnt := 0.U
                state := s_receiving
            }
        }
        is(s_receiving) {
            when(keynode.valid) {
                cnt := cnt + 1.U
            }
            when(cnt === 4.U){
                cnt := 0.U
            }
            when(cnt === 1.U){
                io.mouse_left_click := keycode[7]
            }.otherwise{
                io.mouse_left_click := false.B
            }
        }
    }
}