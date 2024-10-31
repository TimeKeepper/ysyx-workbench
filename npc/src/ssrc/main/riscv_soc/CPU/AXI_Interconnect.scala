package riscv_cpu

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class ysyx_23060198_AXI_Interconnect extends Module {
    val io = IO(new Bundle{
        val ls_resq = Input(Bool())
        val if_resq = Input(Bool())
        val IFU = Flipped(AXI4Bundle(CPUAXI4BundleParameters()))
        val LSU = Flipped(AXI4Bundle(CPUAXI4BundleParameters()))
        val AXI = AXI4Bundle(CPUAXI4BundleParameters())
    })

    val s_if :: s_ls :: Nil = Enum(2)

    val state = RegInit(s_if)

    state := MuxLookup(state, s_if)(
        Seq(
            s_if -> Mux(io.ls_resq === true.B, s_ls, s_if),
            s_ls -> Mux(io.if_resq === true.B, s_if, s_ls)
        )
    )
    
    io.AXI.ar.bits.id    := 0.U
    io.AXI.ar.bits.len   := 0.U
    io.AXI.ar.bits.burst := 0.U
    io.AXI.ar.bits.lock  := 0.U
    io.AXI.ar.bits.cache := 0.U
    io.AXI.ar.bits.prot  := 0.U
    io.AXI.ar.bits.qos   := 0.U
    io.AXI.aw.bits.id    := 0.U
    io.AXI.aw.bits.len   := 0.U
    io.AXI.aw.bits.burst := 0.U
    io.AXI.aw.bits.lock  := 0.U
    io.AXI.aw.bits.cache := 0.U
    io.AXI.aw.bits.prot  := 0.U
    io.AXI.aw.bits.qos   := 0.U
    io.AXI.w.bits.last   := 0.U

    when(state === s_if){
        io.IFU <> io.AXI
        io.LSU.ar.ready := false.B
        io.LSU.r.valid := false.B
        io.LSU.r.bits := DontCare
        io.LSU.aw.ready := false.B
        io.LSU.w.ready := false.B
        io.LSU.b.valid := false.B
        io.LSU.b.bits := DontCare
    }.otherwise{
        io.LSU <> io.AXI
        io.IFU.ar.ready := false.B
        io.IFU.r.valid := false.B
        io.IFU.r.bits := DontCare
        io.IFU.aw.ready := false.B
        io.IFU.w.ready := false.B
        io.IFU.b.valid := false.B
        io.IFU.b.bits := DontCare
    }
}