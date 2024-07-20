package ram

import riscv_cpu._
import bus_state._

import chisel3._
import chisel3.util._

class sram_bridge extends BlackBox{
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val valid = Input(Bool())
        val addr  = Input(UInt(32.W))
        val data  = Output(UInt(32.W))
    })
}

class SRAM extends Module {
    val io = IO(new Bundle {
        val araddr = Flipped(Decoupled(new araddr))
        val raddr  = Decoupled(new raddr)
    })

    val state = RegInit(s_wait_valid)
    val LSFR  = RegInit(4.U(32.W))

    when(LSFR === 0.U) {
        LSFR := 4.U
    }.elsewhen(state === s_busy) {
        LSFR := LSFR - 1.U
    }

    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.araddr.valid, s_busy, s_wait_valid),
            s_busy       -> Mux(LSFR === 0.U,   s_wait_ready, s_busy),
            s_wait_ready -> Mux(io.raddr.ready, s_wait_valid, s_wait_ready),
        )
    )

    io.raddr.valid := state === s_wait_ready
    io.araddr.ready := state === s_wait_valid

    val data_cache = RegInit(0.U(32.W))

    val bridge = Module(new sram_bridge)
    bridge.io.clock := clock
    bridge.io.valid := state === s_busy
    bridge.io.addr  := io.araddr.bits.addr
    io.raddr.bits.data := bridge.io.data
    io.raddr.bits.resp := "b0".U
}