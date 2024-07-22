package peripheral

import riscv_cpu._

import chisel3._
import chisel3.util._

class UART_bridge extends BlackBox {
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val valid = Input(Bool())
        val data = Input(UInt(8.W))
    })
}

class UART extends Module{
    val io = IO(new Bundle {
        val AXI = new AXI_Slave
    })

    io.AXI.araddr.ready := false.B
    io.AXI.rdata.valid  := false.B
    io.AXI.rdata.bits.resp := 0.U
    io.AXI.rdata.bits.data := 0.U

    val s_idle :: s_wait_addr :: s_wait_data :: s_wait_resp :: Nil = Enum(4)

    val state_w = RegInit(s_idle)
    val state_cache = RegInit(s_idle)
    state_cache := state_w
    
    state_w := MuxLookup(state_w, s_wait_addr)(
        Seq(
            s_idle      -> Mux(io.AXI.awaddr.valid && io.AXI.wdata.valid, s_wait_resp, s_idle),
            s_wait_addr -> Mux(io.AXI.awaddr.valid, s_wait_resp, s_wait_addr),
            s_wait_data -> Mux(io.AXI.wdata.valid,  s_wait_resp, s_wait_data),
            s_wait_resp -> Mux(io.AXI.bresp.ready, s_idle, s_wait_resp)
        )
    )

    io.AXI.awaddr.ready := state_w === s_wait_addr || state_w === s_idle
    io.AXI.wdata.ready  := state_w === s_wait_data || state_w === s_idle
    io.AXI.bresp.valid  := state_w === s_wait_resp
    io.AXI.bresp.bits.bresp   := 0.U

    val Uart_bridge = Module(new UART_bridge)

    when(state_cache =/= s_wait_resp && state_w === s_wait_resp){
        Uart_bridge.io.valid := true.B
    }.otherwise{
        Uart_bridge.io.valid := false.B
    }

    Uart_bridge.io.clock := clock
    Uart_bridge.io.data := io.AXI.wdata.bits.data
}
