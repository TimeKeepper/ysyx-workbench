package ssrc.Zyqn

import chisel3._
import chisel3.util._

class top extends Module {
    val io = IO(new ssrc.Zyqn.led.waterIO)

    withReset(!(reset.asBool)){
        val water = Module(new ssrc.Zyqn.led.water(50_000_000, 1))
        io <> water.io
    }
}
