package gcd

import chisel3._

/**
  * Compute GCD using subtraction method.
  * Subtracts the smaller from the larger until register y is zero.
  * value in register x is then the GCD
  */
class GCD extends Module {
  val io = IO(new Bundle {
    val value1        = Input(UInt(16.W))
    val value2        = Input(UInt(16.W))
    val loadingValues = Input(Bool(1.W))
    val outputGCD     = Output(UInt(16.W))
    val outputValid   = Output(Bool(1.W))
  })

  val x = Reg(UInt(16.W))
  val y = Reg(UInt(16.W))

  when(x > y) { x := x - y }.otherwise { y := y - x }

  when(io.loadingValues) {
    x := io.value1
    y := io.value2
  }

  io.outputGCD   := x
  io.outputValid := y === 0.U
}
