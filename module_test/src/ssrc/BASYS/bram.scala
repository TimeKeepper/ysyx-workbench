package BASYS

import chisel3._
import chisel3.util._

class BRAM(width_addr: Int, width_data: Int) extends BlackBox{
    val io = IO(new Bundle{
        val clka = Input(Clock())
        val ena = Input(Bool())
        val addra = Input(UInt(width_addr.W))
        val douta = Output(UInt(width_data.W))
    })
}

class BRAM_0(width_addr: Int, width_data: Int) extends BlackBox{
    val io = IO(new Bundle{
        val clka = Input(Clock())
        val ena = Input(Bool())
        val addra = Input(UInt(width_addr.W))
        val douta = Output(UInt(width_data.W))
    })
}

class BRAM_P extends BlackBox{
    val io = IO(new Bundle{
        val clka = Input(Clock())
        val ena = Input(Bool())
        val addra = Input(UInt(9.W))
        val douta = Output(UInt(12.W))
    })
}
