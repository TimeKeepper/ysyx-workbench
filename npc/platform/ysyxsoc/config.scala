package config

import chisel3._
import chisel3.util._

object main_val {
    def Reset_Vector = "h30000000".U(32.W)
}

object Config {
    def DPIC_on: Boolean = true
}
