package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import config._

class PipelineCtrl extends Module {
    val io = IO(new Bundle {
        val IDU_msg = Flipped(ValidIO((new BUS_IDU_2_EXU)))
        val EXU_msg = Flipped(ValidIO((new BUS_EXU_2_WBU)))

        val IDUCtrl = new Pipeline_ctrl
    })

    io.IDUCtrl.flush := false.B
    io.IDUCtrl.stall := (io.IDU_msg.valid && io.EXU_msg.valid) && (io.IDU_msg.bits.GPR_waddr === io.EXU_msg.bits.GPR_waddr) && (io.IDU_msg.bits.GPR_waddr =/= 0.U)
}
