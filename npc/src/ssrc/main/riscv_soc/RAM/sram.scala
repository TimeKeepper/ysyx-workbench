package ram

import riscv_cpu._

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

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

class SRAM(address: Seq[AddressSet], LSFR_delay : UInt)(implicit p: Parameters) extends LazyModule {
    val beatBytes = 4
    val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
        Seq(AXI4SlaveParameters(
            address       = address,
            executable    = true,
            supportsWrite = TransferSizes(1, beatBytes),
            supportsRead  = TransferSizes(1, beatBytes),
            interleavedId = Some(0))
        ),
        beatBytes  = beatBytes)))
        
    lazy val module = new Impl
    class Impl extends LazyModuleImp(this) {

        val AXI = node.in(0)._1
        
        AXI.r.bits.id := RegEnable(AXI.ar.bits.id, AXI.ar.fire)
        AXI.b.bits.id := RegEnable(AXI.aw.bits.id, AXI.aw.fire)
        AXI.r.bits.last := true.B

        val s_idle :: s_wait_addr :: s_wait_data :: s_busy :: s_wait_resp :: Nil = Enum(5)

        val state_r = RegInit(s_wait_addr)
        val state_w = RegInit(s_idle)
        val LSFRr = RegInit(0.U)
        val LSFRw = RegInit(0.U)

        when(LSFRr === 0.U) {
            LSFRr := 0.U
        }.elsewhen(state_r === s_busy) {
            LSFRr := LSFRr - 1.U
        }

        when(LSFRw === 0.U) {
            LSFRw := 0.U
        }.elsewhen(state_w === s_busy) {
            LSFRw := LSFRw - 1.U
        }

        state_r := MuxLookup(state_r, s_wait_addr)(
            Seq(
                s_wait_addr -> Mux(AXI.ar.valid, s_busy, s_wait_addr),
                s_busy       -> Mux(LSFRr === 0.U,  s_wait_resp, s_busy),
                s_wait_resp -> Mux(AXI.r.ready, s_wait_addr, s_wait_resp),
            )
        )

        state_w := MuxLookup(state_w, s_wait_addr)(
            Seq(
                s_idle      -> Mux(AXI.aw.valid && AXI.w.valid, s_busy, s_idle),
                s_wait_addr -> Mux(AXI.aw.valid, s_busy, s_wait_addr),
                s_wait_data -> Mux(AXI.w.valid,  s_busy, s_wait_data),
                s_busy      -> Mux(LSFRw === 0.U,  s_wait_resp, s_busy),
                s_wait_resp -> Mux(AXI.b.ready, s_idle, s_wait_resp)
            )
        )

        AXI.r.valid := state_r === s_wait_resp
        AXI.ar.ready := state_r === s_wait_addr

        AXI.aw.ready := state_w === s_wait_addr || state_w === s_idle
        AXI.w.ready  := state_w === s_wait_data || state_w === s_idle
        AXI.b.valid  := state_w === s_wait_resp

        val bridge = Module(new sram_bridge)
        bridge.io.clock := clock
        bridge.io.read := state_r === s_busy && LSFRr === 0.U
        bridge.io.r_addr  := RegEnable(AXI.ar.bits.addr, AXI.ar.valid && AXI.ar.ready)
        AXI.r.bits.data := bridge.io.r_data
        AXI.r.bits.resp := "b0".U

        bridge.io.write := state_w === s_busy && LSFRw === 0.U
        bridge.io.w_addr  := RegEnable(AXI.aw.bits.addr, AXI.aw.valid && AXI.aw.ready)
        bridge.io.w_data  := RegEnable(AXI.w.bits.data, AXI.w.valid && AXI.w.ready)
        bridge.io.w_strb  := AXI.w.bits.strb
        AXI.b.bits.resp := "b0".U

        val state_rcache = RegInit(s_wait_addr)
        state_rcache := state_r
        val state_wcache = RegInit(s_wait_addr)
        state_wcache := state_w
    }
}
