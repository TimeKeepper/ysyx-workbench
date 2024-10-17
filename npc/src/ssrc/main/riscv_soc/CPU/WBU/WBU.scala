package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._

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
    
    when(io.EXU_2_WBU.valid && io.EXU_2_WBU.ready){
        io.WBU_2_REG.inst_valid := true.B
    }.otherwise{
        io.WBU_2_REG.inst_valid := false.B
    }
    
    val Default_Next_Pc = io.EXU_2_WBU.bits.PC + 4.U

    val Next_Pc = MuxLookup(io.EXU_2_WBU.bits.Branch, io.EXU_2_WBU.bits.Jmp_Pc)(Seq(
        Bran_Jeq -> Mux(io.EXU_2_WBU.bits.Result === 0.U, io.EXU_2_WBU.bits.Jmp_Pc, Default_Next_Pc),
        Bran_Jne -> Mux(io.EXU_2_WBU.bits.Result === 0.U, Default_Next_Pc, io.EXU_2_WBU.bits.Jmp_Pc),
        Bran_Jlt -> Mux(io.EXU_2_WBU.bits.Result(0), io.EXU_2_WBU.bits.Jmp_Pc, Default_Next_Pc),
        Bran_Jge -> Mux(io.EXU_2_WBU.bits.Result(0), Default_Next_Pc, io.EXU_2_WBU.bits.Jmp_Pc),
        Bran_NJmp -> Default_Next_Pc,
    ))

    val GPR_wdata = MuxLookup(io.EXU_2_WBU.bits.MemtoReg, io.EXU_2_WBU.bits.Result)(Seq(
        Y  -> io.EXU_2_WBU.bits.Mem_rdata,
        N  -> Mux(io.EXU_2_WBU.bits.csr_ctr === CSR_N, io.EXU_2_WBU.bits.Result, io.EXU_2_WBU.bits.CSR_rdata),
    ))

    val CSR_waddra = MuxLookup(io.EXU_2_WBU.bits.csr_ctr, io.EXU_2_WBU.bits.CSR_waddr)(Seq(
        CSR_R1W2 -> "h341".U
    ))

    val CSR_wdataa = MuxLookup(io.EXU_2_WBU.bits.csr_ctr, io.EXU_2_WBU.bits.Result)(Seq(
        CSR_R1W2 -> io.EXU_2_WBU.bits.PC,
    ))

    io.WBU_2_REG.Next_Pc       := io.EXU_2_WBU.bits.Next_Pc
    io.WBU_2_REG.GPR_waddr     := io.EXU_2_WBU.bits.GPR_waddr
    io.WBU_2_REG.GPR_wdata     := GPR_wdata
    io.WBU_2_REG.GPR_wen       <> io.EXU_2_WBU.bits.RegWr
    io.WBU_2_REG.CSR_ctr       <> io.EXU_2_WBU.bits.csr_ctr
    io.WBU_2_REG.CSR_waddra    := CSR_waddra
    io.WBU_2_REG.CSR_waddrb    := "h342".U
    io.WBU_2_REG.CSR_wdataa    := CSR_wdataa
    io.WBU_2_REG.CSR_wdatab    := 11.U
}