package peripheral

import riscv_cpu._

import chisel3._
import chisel3.util._

class CLINT extends Module{
    val io = IO(new Bundle {
        val AXI = new AXI_Slave
    })

    io.AXI.awaddr.ready := false.B
    io.AXI.wdata.ready := false.B
    io.AXI.bresp.valid := false.B
    io.AXI.bresp.bits.bresp := 0.U

    val s_wait_valid :: s_wait_ready :: Nil = Enum(2)

    val state = RegInit(s_wait_valid)

    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.AXI.araddr.valid, s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.AXI.rdata.ready, s_wait_valid, s_wait_ready)
        )
    )

    io.AXI.araddr.ready := (state === s_wait_valid)
    io.AXI.rdata.valid := (state === s_wait_ready)

    val mtime = RegInit(0.U(64.W))
    val m_counter = RegInit(0.U(64.W))

    m_counter := m_counter + 1.U
    when(m_counter === 800.U){//目前npc能够跑到800MHz
        m_counter := 0.U
        mtime := mtime + 1.U
    }

    io.AXI.rdata.bits.data := MuxLookup(io.AXI.araddr.bits.addr, 0.U)(
        Seq(
            "ha0000048".U -> mtime(63, 32),
            "ha000004c".U -> mtime(31, 0)
        )
    )

    io.AXI.rdata.bits.resp := 0.U
}