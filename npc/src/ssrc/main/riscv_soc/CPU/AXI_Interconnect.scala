package riscv_cpu

import chisel3._
import chisel3.util._

class AXI_Interconnect extends Module {
    val io = IO(new Bundle{
        val IFU = new AXI_Slave
        val SRAM = new AXI_Master
    })

    io.IFU <> io.SRAM
}