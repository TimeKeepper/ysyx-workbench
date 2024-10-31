package npc

import riscv_cpu._
import ram._
import peripheral._

import chisel3._
import chisel3.util._

class FIX_AXI_BUS_Master extends Bundle{
  val awready = Input(Bool())
  val awvalid = Output(Bool())
  val awaddr  = Output(UInt(32.W))
  val awid    = Output(UInt(4.W))
  val awlen   = Output(UInt(8.W))
  val awsize  = Output(UInt(3.W))
  val awburst = Output(UInt(2.W))

  val wready = Input(Bool())
  val wvalid = Output(Bool())
  val wdata  = Output(UInt(32.W))
  val wstrb  = Output(UInt(4.W))
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
  val rdata  = Input(UInt(32.W))
  val rlast  = Input(Bool())
  val rid    = Input(UInt(4.W))
}

class FIX_AXI_BUS_Slave extends Bundle{
  val awready = Output(Bool())
  val awvalid = Input(Bool())
  val awaddr  = Input(UInt(32.W))
  val awid    = Input(UInt(4.W))
  val awlen   = Input(UInt(8.W))
  val awsize  = Input(UInt(3.W))
  val awburst = Input(UInt(2.W))

  val wready = Output(Bool())
  val wvalid = Input(Bool())
  val wdata  = Input(UInt(32.W))
  val wstrb  = Input(UInt(4.W))
  val wlast  = Input(Bool())

  val bready = Input(Bool())
  val bvalid = Output(Bool())
  val bresp  = Output(UInt(2.W))
  val bid    = Output(UInt(4.W))

  val arready = Output(Bool())
  val arvalid = Input(Bool())
  val araddr  = Input(UInt(32.W))
  val arid    = Input(UInt(4.W))
  val arlen   = Input(UInt(8.W))
  val arsize  = Input(UInt(3.W))
  val arburst = Input(UInt(2.W))

  val rready = Input(Bool())
  val rvalid = Output(Bool())
  val rresp  = Output(UInt(2.W))
  val rdata  = Output(UInt(32.W))
  val rlast  = Output(Bool())
  val rid    = Output(UInt(4.W))
}

class top extends Module {
  val CPU = Module(new ysyx_23060198)

  val Xbar = Module(new Xbar)
  val SRAM = Module(new SRAM(1.U))
  val UART = Module(new UART)
  val CLINT = Module(new CLINT)

  UART.io.AXI <> Xbar.io.UART
  SRAM.io.AXI <> Xbar.io.SRAM
  CLINT.io.AXI <> Xbar.io.CLINT
  
  Xbar.io.AXI.araddr.ready <> CPU.io.master.ar.ready
  Xbar.io.AXI.araddr.valid <> CPU.io.master.ar.valid
  Xbar.io.AXI.araddr.bits.addr <> CPU.io.master.ar.bits.addr
  Xbar.io.AXI.araddr.bits.size <> CPU.io.master.ar.bits.size

  Xbar.io.AXI.rdata.ready  <> CPU.io.master.r.ready
  Xbar.io.AXI.rdata.valid  <> CPU.io.master.r.valid
  Xbar.io.AXI.rdata.bits.data  <> CPU.io.master.r.bits.data
  Xbar.io.AXI.rdata.bits.resp  <> CPU.io.master.r.bits.resp

  Xbar.io.AXI.awaddr.ready <> CPU.io.master.aw.ready
  Xbar.io.AXI.awaddr.valid <> CPU.io.master.aw.valid
  Xbar.io.AXI.awaddr.bits.addr <> CPU.io.master.aw.bits.addr
  Xbar.io.AXI.awaddr.bits.size <> CPU.io.master.aw.bits.size

  Xbar.io.AXI.wdata.ready  <> CPU.io.master.w.ready
  Xbar.io.AXI.wdata.valid  <> CPU.io.master.w.valid
  Xbar.io.AXI.wdata.bits.data  <> CPU.io.master.w.bits.data
  Xbar.io.AXI.wdata.bits.strb  <> CPU.io.master.w.bits.strb

  Xbar.io.AXI.bresp.ready  <> CPU.io.master.b.ready
  Xbar.io.AXI.bresp.valid  <> CPU.io.master.b.valid
  Xbar.io.AXI.bresp.bits.bresp  <> CPU.io.master.b.bits.resp
  CPU.io.master.b.bits.id        := 0.U
  CPU.io.master.r.bits.last      := 0.U
  CPU.io.master.r.bits.id        := 0.U

  CPU.io.slave <> DontCare
  CPU.io.interrupt <> DontCare

  // Xbar.io.AXI <> CPU.io.AXI
}
