package peripheral

import riscv_cpu._

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class CLINT extends Module{
    val io = IO(new Bundle {
        val AXI = Flipped(AXI4Bundle(CPUAXI4BundleParameters()))
    })

    io.AXI.aw.ready := false.B
    io.AXI.w.ready := false.B
    io.AXI.b.valid := false.B
    io.AXI.b.bits.resp := 0.U

    io.AXI.r.bits.id := RegEnable(io.AXI.ar.bits.id, io.AXI.ar.fire)
    io.AXI.b.bits.id := RegEnable(io.AXI.aw.bits.id, io.AXI.aw.fire)
    io.AXI.r.bits.last := true.B

    val s_wait_valid :: s_wait_ready :: Nil = Enum(2)

    val state = RegInit(s_wait_valid)

    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.AXI.ar.valid, s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.AXI.r.ready, s_wait_valid, s_wait_ready)
        )
    )

    io.AXI.ar.ready := (state === s_wait_valid)
    io.AXI.r.valid := (state === s_wait_ready)

    val mtime = RegInit(0.U(64.W))
    val m_counter = RegInit(0.U(64.W))

    m_counter := m_counter + 1.U
    when(m_counter === 800.U){//目前npc能够跑到800MHz
        m_counter := 0.U
        mtime := mtime + 1.U
    }

    io.AXI.r.bits.data := MuxLookup(io.AXI.ar.bits.addr, 0.U)(
        Seq(
            "ha0000048".U -> mtime(63, 32),
            "ha000004c".U -> mtime(31, 0)
        )
    )

    io.AXI.r.bits.resp := 0.U
}