package BASYS

import chisel3._
import chisel3.util._

class VGASyncIO extends Bundle{
	val hsync = Output(Bool())
	val vsync = Output(Bool())
	val valid = Output(Bool())
}

class vga_sync extends RawModule{
	val io = IO(new VGASyncIO)
	val clock = IO(Input(Clock()))
	val reset = IO(Input(Reset()))
	val xaddr, yaddr = IO(Output(UInt(10.W)))

	def h_front_porch = 96.U
	def h_active = 144.U
	def h_backporch = 784.U
	def h_total = 800.U

	def v_front_porch = 2.U
	def v_active = 35.U
	def v_backporch = 515.U
	def v_total = 525.U

	withClockAndReset(clock, reset){

		val x_cnt = RegInit(1.U(10.W))
		val y_cnt = RegInit(1.U(10.W))

		when(x_cnt >= h_total){
			x_cnt := 1.U
		}.otherwise{
			x_cnt := x_cnt + 1.U
		}

		when(y_cnt >= v_total){
			y_cnt := 1.U
		}.elsewhen(x_cnt >= h_total){
			y_cnt := y_cnt + 1.U
		}

		io.hsync := (x_cnt > h_front_porch)
		io.vsync := (y_cnt > v_front_porch)

		val h_valid = Wire(Bool())
		val v_valid = Wire(Bool())
		h_valid := (x_cnt > h_active) & (x_cnt <= h_backporch)
		v_valid := (y_cnt > v_active) & (y_cnt <= v_backporch)
		io.valid := h_valid & v_valid

		xaddr := Mux(h_valid, x_cnt - h_active - 1.U, 0.U)
		yaddr := Mux(v_valid, y_cnt - v_active - 1.U, 0.U)
	}
}
