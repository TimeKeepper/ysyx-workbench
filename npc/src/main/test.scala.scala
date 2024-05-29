package gcd

import chisel3._

class test() extends Module {
  val io = IO(new Bundle {
    val value1        = Input(UInt(16.W))
    val value2        = Input(UInt(16.W))
    val loadingValues = Input(Bool())
    val outputGCD     = Output(UInt(16.W))
    val outputValid   = Output(Bool())
  })
  val gcd = Module(new DecoupledGcd(16))

  gcd.input.bits.value1 := io.value1
  gcd.input.bits.value2 := io.value2
  gcd.input.ready := io.loadingValues

  io.outputGCD := gcd.output.bits.gcd
  io.outputValid := gcd.output.valid
}