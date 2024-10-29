package config

import chisel3._
import chisel3.util._

object Config {
  var Reset_Vector = "h80000000".U(32.W)

  var DPIC_on: Boolean = false

  def setResetVector(addr: UInt): Unit = {
    Reset_Vector := addr
  }
  def setDPIC(on: Boolean): Unit = {
    DPIC_on = on
  }
}
