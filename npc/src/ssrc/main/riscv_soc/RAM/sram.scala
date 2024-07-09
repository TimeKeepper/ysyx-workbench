package ram

import chisel3._
import chisel3.util._

//此模块将32为数据读取并根据memop处理数据，延迟一个周期后发送给cpu

class SRAM extends Module {
    val io = IO(new Bundle{
        val inst_input = Input(UInt(32.W))
        val inst_output = Output(UInt(32.W))
    })
    io.inst_output := io.inst_input
}