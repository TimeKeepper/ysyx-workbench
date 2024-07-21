package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._

// riscv load store unit

class LSU extends Module{
    val io = IO(new Bundle{
        val GPR_Adata = Input(UInt(32.W))
        val GPR_Bdata = Input(UInt(32.W))
        val IMM       = Input(UInt(32.W))

        val Mem_wraddr = Output(UInt(32.W))
        val Mem_wdata  = Output(UInt(32.W))
    })

    io.Mem_wraddr := io.GPR_Adata + io.IMM
    io.Mem_wdata  := io.GPR_Bdata
}