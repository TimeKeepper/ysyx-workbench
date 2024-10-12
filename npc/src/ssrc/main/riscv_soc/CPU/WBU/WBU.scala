package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._

class ysyx_23060198_WBU extends Module {
    val io = IO(new Bundle{
        val EXU_2_WBU = Flipped(Decoupled(Input(new BUS_EXU_2_WBU)))
        val WBU_2_IFU = Decoupled(Output(new BUS_WBU_2_IFU))
        val WBU_2_REG = Output(new BUS_WBU_2_REG)
    })

    val state = RegInit(s_wait_ready)

    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.EXU_2_WBU.valid, s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.WBU_2_IFU.ready, s_wait_valid, s_wait_ready),
        )
    )

    io.WBU_2_IFU.valid := state === s_wait_ready && !reset.asBool // 这是由于soc外设的行为不确定而做出的改动
    io.EXU_2_WBU.ready  := state === s_wait_valid
    
    io.EXU_2_WBU <> io.WBU_2_REG
}