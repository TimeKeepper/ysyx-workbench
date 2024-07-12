package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

// riscv writeback unit

class WBU_input extends Bundle{
    val RegWr    = Input(Bool())
    val Branch   = Input(Bran_Type)
    val MemtoReg = Input(Bool())
    val MemWr    = Input(Bool())
    val MemOp    = Input(MemOp_Type)
    val csr_ctr  = Input(CSR_Type)
    val Imm      = Input(UInt(32.W))
    val GPR_Adata= Input(UInt(32.W))
    val GPR_Bdata= Input(UInt(32.W))
    val GPR_waddr= Input(UInt(5.W))
    val PC       = Input(UInt(32.W))
    val CSR      = Input(UInt(32.W))
    val Result   = Input(UInt(32.W))
    val Zero     = Input(Bool())
    val Less     = Input(Bool())

    val Mem_rdata  = Input(UInt(32.W))
}

class WBU_output extends Bundle{
    val Mem_wraddr = Output(UInt(32.W))
    val Mem_wdata  = Output(UInt(32.W))
    val MemOp    = Output(MemOp_Type)
    val MemWr    = Output(Bool())

    val Next_Pc   = Output(UInt(32.W))
    val GPR_waddr = Output(UInt(5.W))
    val GPR_wdata = Output(UInt(32.W))
    val GPR_wen   = Output(Bool())
    val CSR_ctr   = Output(CSR_Type)
    val CSR_waddra= Output(UInt(12.W))
    val CSR_waddrb= Output(UInt(12.W))
    val CSR_wdataa= Output(UInt(32.W))
    val CSR_wdatab= Output(UInt(32.W))
}

class WBU extends Module {
    val io = IO(new Bundle{
        val in = new WBU_input 
        val out = new WBU_output
    })

    val lsu = Module(new LSU)
    val bcu = Module(new BCU)

    lsu.io.in       <> io.in

    io.out.Mem_wraddr   <> lsu.io.out.Mem_wraddr
    io.out.Mem_wdata    <> lsu.io.out.Mem_wdata 
    io.out.MemOp        <> lsu.io.out.MemOp     
    io.out.MemWr        <> lsu.io.out.MemWr     

    bcu.io.Branch   <> lsu.io.out.Branch
    bcu.io.Zero     <> lsu.io.out.Zero
    bcu.io.Less     <> lsu.io.out.Less
    
    val PCAsrc = Wire(UInt(32.W))
    val PCBsrc = Wire(UInt(32.W))

    PCAsrc := MuxLookup(bcu.io.PCAsrc, 0.U)(Seq(
        PCAsrc_Imm -> lsu.io.out.Imm,
        PCAsrc_0  -> 0.U,
        PCAsrc_4 -> 4.U,
        PCAsrc_csr -> lsu.io.out.CSR,
    ))

    PCBsrc := MuxLookup(bcu.io.PCBsrc, 0.U)(Seq(
        PCBsrc_gpr -> lsu.io.out.GPR_Adata,
        PCBsrc_pc  -> lsu.io.out.PC,
        PCBsrc_0   -> 0.U,
    ))

    io.out.Next_Pc := PCAsrc + PCBsrc

    io.out.GPR_waddr := lsu.io.out.GPR_waddr
    io.out.GPR_wdata := MuxLookup(lsu.io.out.MemtoReg, lsu.io.out.Result)(Seq(
        Y  -> lsu.io.out.Mem_rdata,
        N  -> Mux(lsu.io.out.csr_ctr === CSR_N, lsu.io.out.Result, lsu.io.out.CSR),
    ))
    io.out.GPR_wen <> lsu.io.out.RegWr

    io.out.CSR_ctr <> lsu.io.out.csr_ctr

    io.out.CSR_waddra := MuxLookup(lsu.io.out.csr_ctr, lsu.io.out.Imm(11, 0))(Seq(
        CSR_R1W2 -> "h341".U
    ))

    io.out.CSR_waddrb := "h342".U

    io.out.CSR_wdataa := MuxLookup(lsu.io.out.csr_ctr, lsu.io.out.Result)(Seq(
        CSR_R1W2 -> lsu.io.out.PC,
    ))

    io.out.CSR_wdatab := 11.U
}