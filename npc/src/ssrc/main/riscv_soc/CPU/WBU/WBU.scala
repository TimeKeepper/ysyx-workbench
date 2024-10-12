package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._

// riscv writeback unit

class ysyx_23060198_WBU extends Module {
    val io = IO(new Bundle{
        val EXU_2_WBU = Flipped(Decoupled(Input(new BUS_EXU_2_WBU)))
        val WBU_2_IFU = Decoupled(Output(new BUS_WBU_2_IFU))
        val WBU_2_REG = Output(new BUS_WBU_2_REG)
    })

    val state = RegInit(s_wait_ready)

    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.EXU_2_WBU.valid, s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.WBU_2_IFU.ready, s_wait_valid, s_wait_ready),
        )
    )

    io.WBU_2_IFU.valid := state === s_wait_ready && !reset.asBool // 这是由于soc外设的行为不确定而做出的改动
    io.EXU_2_WBU.ready  := state === s_wait_valid

    val bcu = Module(new ysyx_23060198_BCU)    

    bcu.io.Branch   <> io.EXU_2_WBU.bits.Branch
    bcu.io.Zero     <> io.EXU_2_WBU.bits.Zero
    bcu.io.Less     <> io.EXU_2_WBU.bits.Less
    
    val PCAsrc = Wire(UInt(32.W))
    val PCBsrc = Wire(UInt(32.W))

    PCAsrc := MuxLookup(bcu.io.PCAsrc, 0.U)(Seq(
        PCAsrc_Imm -> io.EXU_2_WBU.bits.Imm,
        PCAsrc_0  -> 0.U,
        PCAsrc_4 -> 4.U,
        PCAsrc_csr -> io.EXU_2_WBU.bits.CSR,
    ))

    PCBsrc := MuxLookup(bcu.io.PCBsrc, 0.U)(Seq(
        PCBsrc_gpr -> io.EXU_2_WBU.bits.GPR_Adata,
        PCBsrc_pc  -> io.EXU_2_WBU.bits.PC,
        PCBsrc_0   -> 0.U,
    ))

    when(io.EXU_2_WBU.valid && io.EXU_2_WBU.ready){
        io.WBU_2_REG.inst_valid := true.B
    }.otherwise{
        io.WBU_2_REG.inst_valid := false.B
    }

    io.WBU_2_REG.Next_Pc := PCAsrc + PCBsrc

    io.WBU_2_REG.GPR_waddr := io.EXU_2_WBU.bits.GPR_waddr
    io.WBU_2_REG.GPR_wdata := MuxLookup(io.EXU_2_WBU.bits.MemtoReg, io.EXU_2_WBU.bits.Result)(Seq(
        Y  -> io.EXU_2_WBU.bits.Mem_rdata,
        N  -> Mux(io.EXU_2_WBU.bits.csr_ctr === CSR_N, io.EXU_2_WBU.bits.Result, io.EXU_2_WBU.bits.CSR),
    ))
    io.WBU_2_REG.GPR_wen <> io.EXU_2_WBU.bits.RegWr

    io.WBU_2_REG.CSR_ctr <> io.EXU_2_WBU.bits.csr_ctr

    io.WBU_2_REG.CSR_waddra := MuxLookup(io.EXU_2_WBU.bits.csr_ctr, io.EXU_2_WBU.bits.Imm(11, 0))(Seq(
        CSR_R1W2 -> "h341".U
    ))

    io.WBU_2_REG.CSR_waddrb := "h342".U

    io.WBU_2_REG.CSR_wdataa := MuxLookup(io.EXU_2_WBU.bits.csr_ctr, io.EXU_2_WBU.bits.Result)(Seq(
        CSR_R1W2 -> io.EXU_2_WBU.bits.PC,
    ))

    io.WBU_2_REG.CSR_wdatab := 11.U
}