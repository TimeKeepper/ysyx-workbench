package ram

import riscv_cpu._

import chisel3._
import chisel3.util._

class sram_bridge extends BlackBox{
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val read  = Input(Bool())
        val r_addr  = Input(UInt(32.W))
        val r_data  = Output(UInt(32.W))
        val write = Input(Bool())
        val w_addr  = Input(UInt(32.W))
        val w_data  = Input(UInt(32.W))
        val w_strb  = Input(UInt(4.W))
    })
}

class SRAM(val LSFR_delay : UInt) extends Module {
    val io = IO(new Bundle {
        val araddr = Flipped(Decoupled(new araddr))
        val raddr  = Decoupled(new raddr)
        val awaddr = Flipped(Decoupled(new awaddr))
        val wdata  = Flipped(Decoupled(new wdata))
        val bresp  = Decoupled(new bresp)
    })

    val s_wait_addr :: s_wait_data :: s_busy :: s_wait_resp :: Nil = Enum(4)

    val state_r, state_w = RegInit(s_wait_addr)
    val LSFRr = RegInit(LSFR_delay)
    val LSFRw = RegInit(LSFR_delay)

    when(LSFRr === 0.U) {
        LSFRr := LSFR_delay
    }.elsewhen(state_r === s_busy) {
        LSFRr := LSFRr - 1.U
    }

    when(LSFRw === 0.U) {
        LSFRw := LSFR_delay
    }.elsewhen(state_r === s_busy) {
        LSFRw := LSFRr - 1.U
    }

    state_r := MuxLookup(state_r, s_wait_addr)(
        Seq(
            s_wait_addr -> Mux(io.araddr.valid, s_busy, s_wait_addr),
            s_busy       -> Mux(LSFRr === 0.U,  s_wait_resp, s_busy),
            s_wait_resp -> Mux(io.raddr.ready, s_wait_addr, s_wait_resp),
        )
    )

    state_w := MuxLookup(state_w, s_wait_addr)(
        Seq(
            s_wait_addr -> Mux(io.awaddr.valid, s_wait_data, s_wait_addr),
            s_wait_data -> Mux(io.wdata.valid,  s_busy, s_wait_data),
            s_busy      -> Mux(LSFRw === 0.U,  s_wait_resp, s_busy),
            s_wait_resp -> Mux(io.bresp.ready, s_wait_addr, s_wait_resp)
        )
    )

    io.raddr.valid := state_r === s_wait_resp
    io.araddr.ready := state_r === s_wait_addr

    io.awaddr.ready := state_w === s_wait_addr
    io.wdata.ready  := state_w === s_wait_data
    io.bresp.valid  := state_w === s_wait_resp

    val bridge = Module(new sram_bridge)
    bridge.io.clock := clock
    bridge.io.read := state_r === s_busy && LSFRw === 0.U
    bridge.io.r_addr  := io.araddr.bits.addr
    io.raddr.bits.data := bridge.io.r_data
    io.raddr.bits.resp := "b0".U

    bridge.io.write := state_w === s_busy && LSFRw === 0.U
    bridge.io.w_addr  := io.awaddr.bits.addr
    bridge.io.w_data  := io.wdata.bits.data
    bridge.io.w_strb  := io.wdata.bits.strb
    io.bresp.bits.bresp := "b0".U
}