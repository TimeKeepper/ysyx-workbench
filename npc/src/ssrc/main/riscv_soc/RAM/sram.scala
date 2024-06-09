package sram

import chisel3._
import chisel3.util._

//此模块将32为数据读取并根据memop处理数据，延迟一个周期后发送给cpu

class SRAM extends Module {
    val io = IO(new Bundle{
        val inst_input = Flipped(Decoupled(UInt(32.W)))
        val inst_output = Decoupled(UInt(32.W))
    })

    val s_idle :: s_wait_ready :: Nil = Enum(2)
    val state = RegInit(s_idle)

    io.inst_input.ready := state === s_idle
    io.inst_output.valid := io.inst_input.valid && state === s_wait_ready

    when(state === s_idle) {
        when(io.inst_output.valid) {
            state := s_wait_ready
        }.otherwise {
            state := s_idle
        }
    }.otherwise {
        when(io.inst_output.ready){
            state := s_idle
        }
    }

    io.inst_output.bits := io.inst_input.bits
}