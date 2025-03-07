package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import config._
import freechips.rocketchip.tilelink.TLMessages.d

class PipelineCtrl extends Module {
    val io = IO(new Bundle {
        val GPR_read = Flipped(ValidIO((new BUS_IDU_2_REG)))

        val IFU_out = Flipped(ValidIO(new BUS_IFU_2_IDU))
        val IDU_in  = Flipped(ValidIO((new BUS_IFU_2_IDU)))
        val EXU_in  = Flipped(ValidIO((new BUS_IDU_2_EXU)))
        val WBU_in  = Flipped(ValidIO((new BUS_EXU_2_WBU)))
        val Branch_msg = Flipped(ValidIO((new BUS_WBU_2_IFU)))

        val IFUCtrl = new Pipeline_ctrl
        val IDUCtrl = new Pipeline_ctrl
        val EXUCtrl = new Pipeline_ctrl
    })

    def conflict(rs: UInt, rd: UInt) = (rs === rd)

    def conflict_gpr(rs: UInt, rd:UInt) = (conflict(rs, rd) && (rs =/= 0.U))
    def conflict_gpr_valid(rs: UInt) = 
        (conflict_gpr(rs, io.EXU_in.bits.GPR_waddr) & io.EXU_in.valid) ||
        (conflict_gpr(rs, io.WBU_in.bits.GPR_waddr) & io.WBU_in.valid)

    def is_gpr_RAW = io.GPR_read.valid && 
                     (conflict_gpr_valid(io.GPR_read.bits.GPR_Aaddr) ||
                     conflict_gpr_valid(io.GPR_read.bits.GPR_Baddr))

    def conflict_pc(target: UInt) =
        io.Branch_msg.valid && (target =/= io.Branch_msg.bits.Next_PC)

    def is_bp_error = MuxCase(conflict_pc(io.IFU_out.bits.PC), Seq(
        (io.EXU_in.valid -> conflict_pc(io.EXU_in.bits.PC)),
        (io.IDU_in.valid -> conflict_pc(io.IDU_in.bits.PC)),
    ))

    io.IFUCtrl.flush := is_bp_error
    io.IFUCtrl.stall := false.B

    io.IDUCtrl.flush := is_bp_error
    io.IDUCtrl.stall := is_gpr_RAW

    io.EXUCtrl.flush := is_bp_error
    io.EXUCtrl.stall := false.B
}
