package riscv_cpu

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import org.chipsalliance.cde.config.{Parameters, Config}
import freechips.rocketchip.system._
import freechips.rocketchip.diplomacy.LazyModule

class Xbar extends Module{
    val io = IO(new Bundle{
        val AXI = Flipped(AXI4Bundle(CPUAXI4BundleParameters()))
        val SRAM = AXI4Bundle(CPUAXI4BundleParameters())
        val UART = AXI4Bundle(CPUAXI4BundleParameters())
        val CLINT = AXI4Bundle(CPUAXI4BundleParameters())
    })

    when(io.AXI.aw.bits.addr === "h10000000".U){
        io.UART <> io.AXI
        io.SRAM <> DontCare
        io.SRAM.ar.valid := false.B
        io.SRAM.r.ready := false.B
        io.SRAM.aw.valid := false.B
        io.SRAM.w.valid := false.B
        io.SRAM.b.ready := false.B
        io.CLINT <> DontCare
        io.CLINT.ar.valid := false.B
        io.CLINT.r.ready := false.B
        io.CLINT.aw.valid := false.B
        io.CLINT.w.valid := false.B
        io.CLINT.b.ready := false.B
    }.elsewhen(io.AXI.aw.bits.addr === "ha0000048".U || io.AXI.aw.bits.addr === "ha000004c".U ){
        io.CLINT <> io.AXI
        io.SRAM <> DontCare
        io.SRAM.ar.valid := false.B
        io.SRAM.r.ready := false.B
        io.SRAM.aw.valid := false.B
        io.SRAM.w.valid := false.B
        io.SRAM.b.ready := false.B
        io.UART <> DontCare
        io.UART.ar.valid := false.B
        io.UART.r.ready := false.B
        io.UART.aw.valid := false.B
        io.UART.w.valid := false.B
        io.UART.b.ready := false.B
    }.otherwise{
        io.SRAM <> io.AXI
        io.CLINT <> DontCare
        io.CLINT.ar.valid := false.B
        io.CLINT.r.ready := false.B
        io.CLINT.aw.valid := false.B
        io.CLINT.w.valid := false.B
        io.CLINT.b.ready := false.B
        io.UART <> DontCare
        io.UART.ar.valid := false.B
        io.UART.r.ready := false.B
        io.UART.aw.valid := false.B
        io.UART.w.valid := false.B
        io.UART.b.ready := false.B
    }
}