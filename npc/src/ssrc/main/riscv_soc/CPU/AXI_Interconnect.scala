package riscv_cpu

import chisel3._
import chisel3.util._

class ysyx_23060198_AXI_Interconnect extends Module {
    val io = IO(new Bundle{
        val ls_resq = Input(Bool())
        val if_resq = Input(Bool())
        val IFU = new AXI_Slave
        val LSU = new AXI_Slave
        val AXI = new AXI_Master
    })

    val s_if :: s_ls :: Nil = Enum(2)

    val state = RegInit(s_if)

    state := MuxLookup(state, s_if)(
        Seq(
            s_if -> Mux(io.ls_resq === true.B, s_ls, s_if),
            s_ls -> Mux(io.if_resq === true.B, s_if, s_ls)
        )
    )

    when(state === s_if){
        io.IFU <> io.AXI
        io.LSU.rdata.valid := false.B
        io.LSU.rdata.bits := DontCare
        io.LSU.araddr.ready := false.B
        io.LSU.awaddr.ready := false.B
        io.LSU.wdata.ready := false.B
        io.LSU.bresp.valid := false.B
        io.LSU.bresp.bits := DontCare
    }.otherwise{
        io.LSU <> io.AXI
        io.IFU.rdata.valid := false.B
        io.IFU.rdata.bits := DontCare
        io.IFU.araddr.ready := false.B
        io.IFU.awaddr.ready := false.B
        io.IFU.wdata.ready := false.B
        io.IFU.bresp.valid := false.B
        io.IFU.bresp.bits := DontCare
    }
}