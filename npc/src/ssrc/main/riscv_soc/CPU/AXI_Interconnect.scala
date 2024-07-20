package riscv_cpu

import chisel3._
import chisel3.util._

class AXI_Slave extends Bundle{
    val araddr = Decoupled(new araddr)
    val raddr = Flipped(Decoupled(new raddr))
    val awaddr = Decoupled(new awaddr)
    val wdata = Decoupled(new wdata)
    val bresp  = Flipped(Decoupled(new bresp))
}

class AXI_Master extends Bundle{
    val araddr = Flipped(Decoupled(new araddr))
    val raddr = Decoupled(new raddr)
    val awaddr = Flipped(Decoupled(new awaddr))
    val wdata = Flipped(Decoupled(new wdata))
    val bresp  = Decoupled(new bresp)
}

class AXI_Interconnect extends Module {
    val io = IO(new Bundle{
        val IFU = new AXI_Master
        val SRAM = new AXI_Slave
    })

    io.IFU <> io.SRAM
}