package riscv_cpu

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

import signal_value._

// riscv writeback unit

class WBU extends Module {
    val io = IO(new Bundle{
        val RegWr    = Input(Bool())
        val Branch   = Input(Bran_Type)
        val MemtoReg = Input(Bool())
        val MemWr    = Input(Bool())
        val MemOp    = Input(MemOp_Type)
        val csr_ctr  = Input(CSR_Type)
        val PC       = Input(UInt(32.W))
        val CSR      = Input(UInt(32.W))
        val Result   = Input(UInt(32.W))
        val Zero     = Input(Bool())
        val Less     = Input(Bool())
        val GPR_Adata= Input(UInt(32.W))
        val GPR_Bdata= Input(UInt(32.W))
        val GPR_waddr= Input(UInt(5.W))
        val IMM      = Input(UInt(32.W))

        val Mem_wraddr = Output(UInt(32.W))
        val Mem_rdata  = Input(UInt(32.W))
        val Mem_wdata  = Output(UInt(32.W))
        val MemOp_o    = Output(MemOp_Type)
        val MemWr_o    = Input(Bool())

        val Next_Pc   = Output(UInt(32.W))
        val Reg_waddr = Output(UInt(5.W))
        val Reg_wdata = Output(UInt(32.W))
        val Reg_wen   = Output(Bool())
        val CSR_ctr_o = Output(CSR_Type)
        val CSR_waddra= Output(UInt(12.W))
        val CSR_waddrb= Output(UInt(12.W))
        val CSR_wdataa= Output(UInt(32.W))
        val CSR_wdatab= Output(UInt(32.W))
    })

    io.Mem_wraddr <> io.Result
    io.Mem_wdata  <> io.GPR_Bdata
    io.MemOp_o    <> io.MemOp
    io.MemWr_o    <> io.MemWr

    val bcu = Module(new BCU)

    bcu.io.Branch   <> io.Branch
    bcu.io.Zero     <> io.Zero
    bcu.io.Less     <> io.Less
    
    val PCAsrc = Wire(UInt(32.W))
    val PCBsrc = Wire(UInt(32.W))

    PCAsrc := Muxlookup(bcu.io.PCAsrc, 0.U)(Seq(
        A_RS1 -> GPR_Adata,
        A_PC  -> io.PC,
        A_CSR -> io.CSR,
    ))

    PCBsrc := Muxlookup(bcu.io.PCBsrc, 0.U)(Seq(
        B_RS2 -> GPR_Bdata,
        B_IMM -> io.IMM,
        B_4   -> 4.U,
        B_RS1 -> GPR_Adata,
    ))

    io.Next_Pc := PCAsrc + PCBsrc

    io.Reg_waddr := io.GPR_waddr
    io.Reg_wdata := Muxlookup(io.MemtoReg, io.Result)(Seq(
        Y  -> io.mem_rdata,
        N  -> io.Result,
    ))
    io.Reg_wen <> io.RegWr

    io.CSR_ctr_o <> io.csr_ctr

    io.CSR_waddra := Muxlookup(io.csr_ctr, "h341".U)(Seq(
        CSR_R1W2 -> io.imm(11, 0)
    ))

    io.CSR_waddrb := "h342".U

    io.CSR_wdataa := Muxlookup(io.csr_ctr, io.GPR_Adata)(Seq(
        CSR_R1W2 -> io.PC,
    ))

    io.CSR_wdatab := 11.U
}