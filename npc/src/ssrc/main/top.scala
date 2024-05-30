package top

import chisel3._
import gcd._

class top extends Module {
  val io = IO(new Bundle {
    val value1        = Input(UInt(16.W)) 
    val value2        = Input(UInt(16.W))
    val loadingValues = Input(Bool())
    val outputGCD     = Output(UInt(16.W))
    val outputValid   = Output(Bool())
  })

  val submodule = Module(new GCD)

  submodule.io.value1        := io.value1
  submodule.io.value2        := io.value2
  submodule.io.loadingValues := io.loadingValues

  io.outputGCD   := submodule.io.outputGCD
  io.outputValid := submodule.io.outputValid
}
