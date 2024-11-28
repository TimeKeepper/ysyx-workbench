package config

import chisel3._
import chisel3.util._

object Config {
  var Reset_Vector = "h80000000".U(32.W)

  var DPIC_on: Boolean = false

  var Simulate: Boolean = false

  def setResetVector(addr: UInt): Unit = {
    Reset_Vector := addr
  }

  def setDPIC(on: Boolean): Unit = {
    DPIC_on = on
  }

  def setSimulate(on: Boolean): Unit = {
    Simulate = on
  }

  object Icache_Param {
    var offsetWidth = 2
    var indexWidth = 4
    var tagWidth = 19
    var mapAddr = "ha0000000"
  }

  def setIcacheParam(offsetWidth: Int, indexWidth: Int, tagWidth: Int, mapAddr: String): Unit = {
    Icache_Param.offsetWidth = offsetWidth
    Icache_Param.indexWidth = indexWidth
    Icache_Param.tagWidth = tagWidth
    Icache_Param.mapAddr = mapAddr
  }
}
