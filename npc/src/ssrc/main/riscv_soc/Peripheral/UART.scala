package peripheral

import riscv_cpu._

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class UART_bridge extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val valid = Input(Bool())
        val data = Input(UInt(8.W))
    })
    setInline("UART_bridge.v",
    """module UART_bridge(
      |  input  clock,
      |  input valid,
      |  input [7:0] data
      |);
      |  import "DPI-C" function void my_putc(input int c);
      |  
      |  always @(posedge clock) begin
      |    if(valid) begin
      |      my_putc({24'h0, data});
      |    end
      |  end
      |
      |endmodule
    """.stripMargin)
}

class UART extends Module{
    val io = IO(new Bundle {
        val AXI = Flipped(AXI4Bundle(CPUAXI4BundleParameters()))
    })

    io.AXI.ar.ready := false.B
    io.AXI.r.valid  := false.B
    io.AXI.r.bits.resp := 0.U
    io.AXI.r.bits.data := 0.U
    io.AXI.r.bits.last := true.B

    io.AXI.r.bits.id := RegEnable(io.AXI.ar.bits.id, io.AXI.ar.fire)
    io.AXI.b.bits.id := RegEnable(io.AXI.aw.bits.id, io.AXI.aw.fire)

    val s_idle :: s_wait_addr :: s_wait_data :: s_wait_resp :: Nil = Enum(4)

    val state_w = RegInit(s_idle)
    val state_cache = RegInit(s_idle)
    state_cache := state_w
    
    state_w := MuxLookup(state_w, s_wait_addr)(
        Seq(
            s_idle      -> Mux(io.AXI.aw.valid && io.AXI.w.valid, s_wait_resp, s_idle),
            s_wait_addr -> Mux(io.AXI.aw.valid, s_wait_resp, s_wait_addr),
            s_wait_data -> Mux(io.AXI.w.valid,  s_wait_resp, s_wait_data),
            s_wait_resp -> Mux(io.AXI.b.ready, s_idle, s_wait_resp)
        )
    )

    io.AXI.aw.ready := state_w === s_wait_addr || state_w === s_idle
    io.AXI.w.ready  := state_w === s_wait_data || state_w === s_idle
    io.AXI.b.valid  := state_w === s_wait_resp
    io.AXI.b.bits.resp   := 0.U

    val Uart_bridge = Module(new UART_bridge)

    when(state_cache =/= s_wait_resp && state_w === s_wait_resp){
        Uart_bridge.io.valid := true.B
    }.otherwise{
        Uart_bridge.io.valid := false.B
    }

    Uart_bridge.io.clock := clock
    Uart_bridge.io.data := io.AXI.w.bits.data
}
