package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._

// riscv excution unit

class ysyx_23060198_EXU extends Module {
    val io = IO(new Bundle{
        // From CSR
        val IDU_2_EXU = Flipped(Decoupled(Input(new BUS_IDU_2_EXU)))
        val REG_2_EXU = Input(new BUS_REG_2_EXU)

        val EXU_2_WBU = Decoupled(Output(new BUS_EXU_2_WBU))
        val AXI = new AXI_Master
    })
    val alu = Module(new ysyx_23060198_ALU)
    val lsu = Module(new ysyx_23060198_LSU)

    when(io.IDU_2_EXU.bits.MemWr || io.IDU_2_EXU.bits.MemtoReg){
        alu.io.IDU_2_EXU.valid := false.B
        alu.io.out.ready := false.B
        io.IDU_2_EXU.ready <> lsu.io.IDU_2_EXU.ready
        io.IDU_2_EXU.valid <> lsu.io.IDU_2_EXU.valid
        io.EXU_2_WBU.ready <> lsu.io.out.ready
        io.EXU_2_WBU.valid <> lsu.io.out.valid
    }.otherwise{
        lsu.io.IDU_2_EXU.valid := false.B
        lsu.io.out.ready := false.B
        io.IDU_2_EXU.ready <> alu.io.IDU_2_EXU.ready
        io.IDU_2_EXU.valid <> alu.io.IDU_2_EXU.valid
        io.EXU_2_WBU.ready <> alu.io.out.ready
        io.EXU_2_WBU.valid <> alu.io.out.valid
    }

    val comunication_succeed = (io.IDU_2_EXU.valid && io.IDU_2_EXU.ready)

    alu.io.IDU_2_EXU.bits := io.IDU_2_EXU.bits
    alu.io.CSR         := io.IDU_2_EXU.bits.CSR_rdata

    lsu.io.IDU_2_EXU.bits := io.IDU_2_EXU.bits
    lsu.io.AXI <> io.AXI
    
    when(io.EXU_2_WBU.valid && io.EXU_2_WBU.ready){
        io.EXU_2_WBU.bits.inst_valid := true.B
    }.otherwise{
        io.EXU_2_WBU.bits.inst_valid := false.B
    }
    
    val PCAsrc = MuxLookup(io.IDU_2_EXU.bits.Branch, Bitpat.Dontcare)(Seq(
        Bran_NJmp -> 4.U,
        Bran_Jmp -> io.IDU_2_EXU.bits.Imm,
        Bran_Jmpr -> io.IDU_2_EXU.bits.Imm,
        Bran_Jeq -> Mux(alu.io.out.bits.Zero, io.IDU_2_EXU.bits.Imm, 4.U),
        Bran_Jne -> Mux(alu.io.out.bits.Zero, 4.U, io.IDU_2_EXU.bits.Imm),
        Bran_Jlt -> Mux(alu.io.out.bits.Less, io.IDU_2_EXU.bits.Imm, 4.U),
        Bran_Jge -> Mux(alu.io.out.bits.Less, 4.U, io.IDU_2_EXU.bits.Imm),
        Bran_Jcsr -> io.IDU_2_EXU.bits.CSR_rdata,
        Bran_NoC -> 0.U,
    ))

    val PCBsrc = MuxLookup(io.IDU_2_EXU.bits.Branch, Bitpat.Dontcare)(Seq(
        Bran_NJmp -> io.IDU_2_EXU.bits.PC,
        Bran_Jmp  -> io.IDU_2_EXU.bits.PC,
        Bran_Jeq  -> io.IDU_2_EXU.bits.PC,
        Bran_Jne  -> io.IDU_2_EXU.bits.PC,
        Bran_Jlt  -> io.IDU_2_EXU.bits.PC,
        Bran_Jge  -> io.IDU_2_EXU.bits.PC,
        Bran_Jmpr -> io.IDU_2_EXU.bits.GPR_Adata,
        Bran_Jcsr -> 0.U,
        Bran_NoC  -> io.IDU_2_EXU.bits.PC,
    ))

    val GPR_wdata = MuxLookup(io.IDU_2_EXU.bits.MemtoReg, alu.io.out.bits.Result)(Seq(
        Y  -> lsu.io.out.bits.Mem_rdata,
        N  -> Mux(io.IDU_2_EXU.bits.csr_ctr === CSR_N, alu.io.out.bits.Result, io.IDU_2_EXU.bits.CSR_rdata),
    ))

    val CSR_waddra = MuxLookup(io.IDU_2_EXU.bits.csr_ctr, io.IDU_2_EXU.bits.Imm(11, 0))(Seq(
        CSR_R1W2 -> "h341".U
    ))

    val CSR_wdataa = MuxLookup(io.IDU_2_EXU.bits.csr_ctr, alu.io.out.bits.Result)(Seq(
        CSR_R1W2 -> io.IDU_2_EXU.bits.PC,
    ))

    io.EXU_2_WBU.bits.Next_Pc       := PCAsrc + PCBsrc
    io.EXU_2_WBU.bits.GPR_waddr     := io.IDU_2_EXU.bits.GPR_waddr
    io.EXU_2_WBU.bits.GPR_wdata     := GPR_wdata
    io.EXU_2_WBU.bits.GPR_wen       <> io.IDU_2_EXU.bits.RegWr
    io.EXU_2_WBU.bits.CSR_ctr       <> io.IDU_2_EXU.bits.csr_ctr
    io.EXU_2_WBU.bits.CSR_waddra    := CSR_waddra
    io.EXU_2_WBU.bits.CSR_waddrb    := "h342".U
    io.EXU_2_WBU.bits.CSR_wdataa    := CSR_wdataa
    io.EXU_2_WBU.bits.CSR_wdatab    := 11.U
}