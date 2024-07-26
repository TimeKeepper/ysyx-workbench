package npc

import riscv_cpu.Xbar
import ram._
import peripheral._

import chisel3._
import chisel3.util._

class FIX_AXI_BUS extends Bundle{
  val awready = Input(Bool())
  val awvalid = Output(Bool())
  val awaddr  = Output(UInt(32.W))
  val awid    = Output(UInt(4.W))
  val awlen   = Output(UInt(8.W))
  val awsize  = Output(UInt(3.W))
  val awburst = Output(UInt(2.W))

  val wready = Input(Bool())
  val wvalid = Output(Bool())
  val wdata  = Output(UInt(64.W))
  val wstrb  = Output(UInt(8.W))
  val wlast  = Output(Bool())

  val bready = Output(Bool())
  val bvalid = Input(Bool())
  val bresp  = Input(UInt(2.W))
  val bid    = Input(UInt(4.W))

  val arready = Input(Bool())
  val arvalid = Output(Bool())
  val araddr  = Output(UInt(32.W))
  val arid    = Output(UInt(4.W))
  val arlen   = Output(UInt(8.W))
  val arsize  = Output(UInt(3.W))
  val arburst = Output(UInt(2.W))

  val rready = Output(Bool())
  val rvalid = Input(Bool())
  val rresp  = Input(UInt(2.W))
  val rdata  = Input(UInt(64.W))
  val rlast  = Input(Bool())
  val rid    = Input(UInt(4.W))
}

class ysyx_23060198 extends BlackBox{
  val io = IO(new Bundle{
    val clock = Input(Clock())
    val reset = Input(Bool())

    val io_Master = new FIX_AXI_BUS

    val io_inst_comp  = Output(Bool())
  })
}

class npc extends Module {
  val io = IO(new Bundle {
    val inst_comp  = Output(Bool())
  })
  
  val CPU = Module(new ysyx_23060198)

  CPU.io.clock := clock
  CPU.io.reset := reset

  val Xbar = Module(new Xbar)
  val SRAM = Module(new SRAM(4.U))
  val UART = Module(new UART)
  val CLINT = Module(new CLINT)

  io.inst_comp   <> CPU.io.io_inst_comp 

  UART.io.AXI <> Xbar.io.UART
  SRAM.io.AXI <> Xbar.io.SRAM
  CLINT.io.AXI <> Xbar.io.CLINT
  
  Xbar.io.AXI.araddr.ready <> CPU.io.io_Master.arready
  Xbar.io.AXI.araddr.valid <> CPU.io.io_Master.arvalid
  Xbar.io.AXI.araddr.bits.addr <> CPU.io.io_Master.araddr

  Xbar.io.AXI.rdata.ready  <> CPU.io.io_Master.rready
  Xbar.io.AXI.rdata.valid  <> CPU.io.io_Master.rvalid
  Xbar.io.AXI.rdata.bits.data  <> CPU.io.io_Master.rdata
  Xbar.io.AXI.rdata.bits.resp  <> CPU.io.io_Master.rresp

  Xbar.io.AXI.awaddr.ready <> CPU.io.io_Master.awready
  Xbar.io.AXI.awaddr.valid <> CPU.io.io_Master.awvalid
  Xbar.io.AXI.awaddr.bits.addr <> CPU.io.io_Master.awaddr

  Xbar.io.AXI.wdata.ready  <> CPU.io.io_Master.wready
  Xbar.io.AXI.wdata.valid  <> CPU.io.io_Master.wvalid
  Xbar.io.AXI.wdata.bits.data  <> CPU.io.io_Master.wdata
  Xbar.io.AXI.wdata.bits.strb  <> CPU.io.io_Master.wstrb

  Xbar.io.AXI.bresp.ready  <> CPU.io.io_Master.bready
  Xbar.io.AXI.bresp.valid  <> CPU.io.io_Master.bvalid
  Xbar.io.AXI.bresp.bits.bresp  <> CPU.io.io_Master.bresp
  CPU.io.io_Master.bid        := 0.U
  CPU.io.io_Master.rlast      := 0.U
  CPU.io.io_Master.rid        := 0.U

  // Xbar.io.AXI <> CPU.io.AXI
}
