package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import config._

class PipelineCtrl extends Module {
    val io = IO(new Bundle {
        val IDU_msg = Flipped(ValidIO((new BUS_IDU_2_REG)))
        val EXU_msg = Flipped(ValidIO((new BUS_IDU_2_EXU)))
        val WBU_msg = Flipped(ValidIO((new BUS_EXU_2_WBU)))

        val IDUCtrl = new Pipeline_ctrl
    })

    def conflict(rs: UInt, rd: UInt) = (rs === rd)

    def conflict_gpr(rs: UInt, rd:UInt) = (conflict(rs, rd) && (rs =/= 0.U))
    def conflict_gpr_valid(rs: UInt) = 
        (conflict_gpr(rs, io.EXU_msg.bits.GPR_waddr) & io.EXU_msg.valid) ||
        (conflict_gpr(rs, io.WBU_msg.bits.GPR_waddr) & io.WBU_msg.valid)

    def is_gpr_RAW = io.IDU_msg.valid && 
                     (conflict_gpr_valid(io.IDU_msg.bits.GPR_Aaddr) ||
                     conflict_gpr_valid(io.IDU_msg.bits.GPR_Baddr))

    io.IDUCtrl.flush := false.B
    io.IDUCtrl.stall := is_gpr_RAW
}
