package riscv_cpu

import chisel3._
import chisel3.util._

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
        val GPR_Bdata= Input(UInt(32.W))

        val Mem_wraddr = Output(UInt(32.W))
        val Mem_rdata  = Input(UInt(32.W))
        val Mem_wdata  = Output(UInt(32.W))
        val MemOp_o    = Output(MemOp_Type)
        val MemWr_o    = Input(Bool())

        val Reg_waddr = Output(UInt(5.W))
        val Reg_wdata = Output(UInt(32.W))
        val Reg_wen   = Output(Bool())
        val Next_Pc   = Output(UInt(32.W))
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
}