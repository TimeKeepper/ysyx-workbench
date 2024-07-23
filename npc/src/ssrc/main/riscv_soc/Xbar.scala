package riscv_cpu

import chisel3._
import chisel3.util._

class Xbar extends Module{
    val io = IO(new Bundle{
        val AXI = new AXI_Slave
        val SRAM = new AXI_Master
        val UART = new AXI_Master
        val CLINT = new AXI_Master
    })

    when(io.AXI.awaddr.bits.addr === "h10000000".U){
        io.UART <> io.AXI
        io.SRAM <> DontCare
        io.SRAM.araddr.valid := false.B
        io.SRAM.rdata.ready := false.B
        io.SRAM.awaddr.valid := false.B
        io.SRAM.wdata.valid := false.B
        io.SRAM.bresp.ready := false.B
        io.CLINT <> DontCare
        io.CLINT.araddr.valid := false.B
        io.CLINT.rdata.ready := false.B
        io.CLINT.awaddr.valid := false.B
        io.CLINT.wdata.valid := false.B
        io.CLINT.bresp.ready := false.B
    }.elsewhen(io.AXI.awaddr.bits.addr === "ha0000048".U || io.AXI.awaddr.bits.addr === "ha000004c".U ){
        io.CLINT <> io.AXI
        io.SRAM <> DontCare
        io.SRAM.araddr.valid := false.B
        io.SRAM.rdata.ready := false.B
        io.SRAM.awaddr.valid := false.B
        io.SRAM.wdata.valid := false.B
        io.SRAM.bresp.ready := false.B
        io.UART <> DontCare
        io.UART.araddr.valid := false.B
        io.UART.rdata.ready := false.B
        io.UART.awaddr.valid := false.B
        io.UART.wdata.valid := false.B
        io.UART.bresp.ready := false.B
    }.otherwise{
        io.SRAM <> io.AXI
        io.CLINT <> DontCare
        io.CLINT.araddr.valid := false.B
        io.CLINT.rdata.ready := false.B
        io.CLINT.awaddr.valid := false.B
        io.CLINT.wdata.valid := false.B
        io.CLINT.bresp.ready := false.B
        io.UART <> DontCare
        io.UART.araddr.valid := false.B
        io.UART.rdata.ready := false.B
        io.UART.awaddr.valid := false.B
        io.UART.wdata.valid := false.B
        io.UART.bresp.ready := false.B
    }
}