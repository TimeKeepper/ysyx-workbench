package ram

import riscv_cpu._

import chisel3._
import chisel3.util._

class sram_bridge extends BlackBox with HasBlackBoxInline {
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
    setInline("SRAM_BRIDGE.v",
    """module sram_bridge(
      |    input  clock,
      |    input  read,
      |    input  [31:0] r_addr,
      |    output reg [31:0] r_data,
      |    input  write,
      |    input  [31:0] w_addr,
      |    input  [31:0] w_data,
      |    input  [3:0]  w_strb
      |);
      |
      |import "DPI-C" function int unsigned paddr_read (input int unsigned addr, input int len);
      |import "DPI-C" function void paddr_write_strb (input int unsigned addr, input int unsigned data, input int mask);
      |
      |    always @(posedge clock) begin
      |        if (read) begin
      |            r_data <= paddr_read(r_addr, 32'd4);
      |        end
      |        if (write) begin
      |            paddr_write_strb(w_addr, w_data, {28'h0, w_strb});
      |        end
      |    end
      |
      |endmodule
    """.stripMargin)
}

class SRAM(val LSFR_delay : UInt) extends Module {
    val io = IO(new Bundle {
        val AXI = new AXI_Slave
    })

    val s_idle :: s_wait_addr :: s_wait_data :: s_busy :: s_wait_resp :: Nil = Enum(5)

    val state_r = RegInit(s_wait_addr)
    val state_w = RegInit(s_idle)
    val LSFRr = RegInit(LSFR_delay - 1.U)
    val LSFRw = RegInit(LSFR_delay - 1.U)

    when(LSFRr === 0.U) {
        LSFRr := LSFR_delay - 1.U
    }.elsewhen(state_r === s_busy) {
        LSFRr := LSFRr - 1.U
    }

    when(LSFRw === 0.U) {
        LSFRw := LSFR_delay - 1.U
    }.elsewhen(state_w === s_busy) {
        LSFRw := LSFRw - 1.U
    }

    state_r := MuxLookup(state_r, s_wait_addr)(
        Seq(
            s_wait_addr -> Mux(io.AXI.araddr.valid, s_busy, s_wait_addr),
            s_busy       -> Mux(LSFRr === 0.U,  s_wait_resp, s_busy),
            s_wait_resp -> Mux(io.AXI.rdata.ready, s_wait_addr, s_wait_resp),
        )
    )

    state_w := MuxLookup(state_w, s_wait_addr)(
        Seq(
            s_idle      -> Mux(io.AXI.awaddr.valid && io.AXI.wdata.valid, s_busy, s_idle),
            s_wait_addr -> Mux(io.AXI.awaddr.valid, s_busy, s_wait_addr),
            s_wait_data -> Mux(io.AXI.wdata.valid,  s_busy, s_wait_data),
            s_busy      -> Mux(LSFRw === 0.U,  s_wait_resp, s_busy),
            s_wait_resp -> Mux(io.AXI.bresp.ready, s_idle, s_wait_resp)
        )
    )

    io.AXI.rdata.valid := state_r === s_wait_resp
    io.AXI.araddr.ready := state_r === s_wait_addr

    io.AXI.awaddr.ready := state_w === s_wait_addr || state_w === s_idle
    io.AXI.wdata.ready  := state_w === s_wait_data || state_w === s_idle
    io.AXI.bresp.valid  := state_w === s_wait_resp

    val bridge = Module(new sram_bridge)
    bridge.io.clock := clock
    bridge.io.read := state_r === s_busy && LSFRr === 0.U
    bridge.io.r_addr  := RegEnable(io.AXI.araddr.bits.addr, io.AXI.araddr.ready && io.AXI.araddr.valid)
    io.AXI.rdata.bits.data := bridge.io.r_data
    io.AXI.rdata.bits.resp := "b0".U

    bridge.io.write := state_w === s_busy && LSFRw === 0.U
    bridge.io.w_addr  := io.AXI.awaddr.bits.addr
    bridge.io.w_data  := io.AXI.wdata.bits.data
    bridge.io.w_strb  := io.AXI.wdata.bits.strb
    io.AXI.bresp.bits.bresp := "b0".U

    val state_rcache = RegInit(s_wait_addr)
    state_rcache := state_r
    val state_wcache = RegInit(s_wait_addr)
    state_wcache := state_w

}