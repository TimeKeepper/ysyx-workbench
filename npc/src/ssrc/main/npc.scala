package npc

import riscv_cpu.Xbar
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


class ysyx_23060198 extends BlackBox{
  val io = IO(new Bundle{
    val clock = Input(Clock())
    val reset = Input(Bool())

    val io_master = new FIX_AXI_BUS_Master
    val io_slave  = new FIX_AXI_BUS_Slave
    val io_interrupt = Input(Bool())
  })
}

class npc extends Module {
  val CPU = Module(new ysyx_23060198)

  CPU.io.clock := clock
  CPU.io.reset := reset

  val Xbar = Module(new Xbar)
  val SRAM = Module(new SRAM(4.U))
  val UART = Module(new UART)
  val CLINT = Module(new CLINT)

  UART.io.AXI <> Xbar.io.UART
  SRAM.io.AXI <> Xbar.io.SRAM
  CLINT.io.AXI <> Xbar.io.CLINT
  
  Xbar.io.AXI.araddr.ready <> CPU.io.io_master.arready
  Xbar.io.AXI.araddr.valid <> CPU.io.io_master.arvalid
  Xbar.io.AXI.araddr.bits.addr <> CPU.io.io_master.araddr
  Xbar.io.AXI.araddr.bits.size <> CPU.io.io_master.arsize

  Xbar.io.AXI.rdata.ready  <> CPU.io.io_master.rready
  Xbar.io.AXI.rdata.valid  <> CPU.io.io_master.rvalid
  Xbar.io.AXI.rdata.bits.data  <> CPU.io.io_master.rdata
  Xbar.io.AXI.rdata.bits.resp  <> CPU.io.io_master.rresp

  Xbar.io.AXI.awaddr.ready <> CPU.io.io_master.awready
  Xbar.io.AXI.awaddr.valid <> CPU.io.io_master.awvalid
  Xbar.io.AXI.awaddr.bits.addr <> CPU.io.io_master.awaddr
  Xbar.io.AXI.awaddr.bits.size <> CPU.io.io_master.awsize

  Xbar.io.AXI.wdata.ready  <> CPU.io.io_master.wready
  Xbar.io.AXI.wdata.valid  <> CPU.io.io_master.wvalid
  Xbar.io.AXI.wdata.bits.data  <> CPU.io.io_master.wdata
  Xbar.io.AXI.wdata.bits.strb  <> CPU.io.io_master.wstrb

  Xbar.io.AXI.bresp.ready  <> CPU.io.io_master.bready
  Xbar.io.AXI.bresp.valid  <> CPU.io.io_master.bvalid
  Xbar.io.AXI.bresp.bits.bresp  <> CPU.io.io_master.bresp
  CPU.io.io_master.bid        := 0.U
  CPU.io.io_master.rlast      := 0.U
  CPU.io.io_master.rid        := 0.U

  CPU.io.io_slave <> DontCare
  CPU.io.io_interrupt <> DontCare

  // Xbar.io.AXI <> CPU.io.AXI
}
