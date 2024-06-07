package  homework

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

class PS2Receiver extends Module {
    val io = IO(new Bundle {
        val kclk = Input(Clock())
        val kdata = Input(UInt(1.W))
        val keycode = Decoupled(UInt(16.W))
    })

    val kclk_f = Wire(Clock())
    val kdata_f = Wire(UInt(1.W))

    val kclk_filiter = Module(new Debouncer(20))
    val kdata_filiter = Module(new Debouncer(20))

    kclk_filiter.io.input := io.kclk
    kclk_f := kclk_filiter.io.output

    kdata_filiter.io.input := io.kclk
    kdata_f := kdata_filiter.io.output

    val data_cur = RegInit(0.U(8.W))
    val data_pre = RegInit(0.U(8.W))
    val cnt = RegInit(0.U(8.W))
    val flag_cur = RegInit(false.B)
    val flag_pre = RegInit(true.B)
    flag_pre := flag_cur

    when(flag_pre === false.B && flag_cur === true.B) {
        io.keycode.bits := Cat(data_pre, data_cur)
        io.keycode.valid := true.B
        data_pre := data_cur
    }.otherwise {
        io.keycode.valid := false.B
    }

    withClock(kclk_f) {
        when(cnt === 10.U){
            cnt := 0.U
        }.otherwise{
            cnt := cnt + 1.U
        }

        data_cur := Cat(kdata_f, data_cur(7,1))

        when(cnt === 9.U) {
            flag_cur := true.B
        }.otherwise {
            flag_cur := false.B
        }
    }

}