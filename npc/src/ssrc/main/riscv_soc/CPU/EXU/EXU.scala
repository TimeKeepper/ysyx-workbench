package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._

// riscv excution unit

class EXU extends Module {
    val io = IO(new Bundle{
        // From CSR
        val in = Flipped(Decoupled(new Bundle{
            val GNU_io    = new GNU_Output
            
            val Mem_rdata = Input(UInt(32.W))

            // Form Register File
            val CSR       = Input(UInt(32.W))
        }))

        val out = Decoupled(new Bundle{
            val EXU_io    = new EXU_output

            val Mem_wraddr = Output(UInt(32.W))
            val Mem_wdata  = Output(UInt(32.W))
            val MemOp      = Output(MemOp_Type)
            val MemWr      = Output(Bool())
        })
    })

    val RegWr_cache       = RegInit(false.B)
    val Branch_cache      = RegInit(Bran_NJmp)
    val MemtoReg_cache    = RegInit(false.B)
    val csr_ctr_cache     = RegInit(CSR_N)
    val Imm_cache         = RegInit(0.U(32.W))
    val GPR_Adata_cache   = RegInit(0.U(32.W))
    val GPR_waddr_cache   = RegInit(0.U(5.W))
    val PC_cache          = RegInit(0.U(32.W))
    val Result_cache      = RegInit(0.U(32.W))
    val Zero_cache        = RegInit(false.B)
    val Less_cache        = RegInit(false.B)
    
    val CSR_cache         = RegInit(0.U(32.W))
    val Mem_rdata_cache   = RegInit(0.U(32.W))  

    val alu = Module(new ALU)
    alu.io.in.valid := true.B
    alu.io.out.ready := true.B
    val lsu = Module(new LSU)
    lsu.io.in.valid := true.B
    lsu.io.out.ready := true.B

    when(io.in.bits.GNU_io.MemWr || io.in.bits.GNU_io.MemtoReg){
        alu.io.in.valid := false.B
        io.in.ready <> lsu.io.in.ready
        io.in.valid <> lsu.io.in.valid
        io.out.ready <> lsu.io.out.ready
        io.out.valid <> lsu.io.out.valid
        io.out.bits.EXU_io <> lsu.io.out.bits.EXU_io
    }.otherwise{
        lsu.io.in.valid := false.B
        io.in.ready <> alu.io.in.ready
        io.in.valid <> alu.io.in.valid
        io.out.ready <> alu.io.out.ready
        io.out.valid <> alu.io.out.valid
        io.out.bits.EXU_io <> alu.io.out.bits.EXU_io
    }

    when(io.in.valid && io.in.ready){
        RegWr_cache       := io.in.bits.GNU_io.RegWr
        Branch_cache      := io.in.bits.GNU_io.Branch
        MemtoReg_cache    := io.in.bits.GNU_io.MemtoReg
        csr_ctr_cache     := io.in.bits.GNU_io.csr_ctr
        Imm_cache         := io.in.bits.GNU_io.Imm
        GPR_Adata_cache   := io.in.bits.GNU_io.GPR_Adata
        GPR_waddr_cache   := io.in.bits.GNU_io.GPR_waddr
        PC_cache          := io.in.bits.GNU_io.PC
        Result_cache      := alu.io.out.bits.EXU_io.Result 
        Zero_cache        := alu.io.out.bits.EXU_io.Zero   
        Less_cache        := alu.io.out.bits.EXU_io.Less  

        CSR_cache         := io.in.bits.CSR
        Mem_rdata_cache   := io.in.bits.Mem_rdata   
    }

    alu.io.in.bits.GNU_io := io.in.bits.GNU_io
    alu.io.in.bits.CSR    := io.in.bits.CSR

    lsu.io.in.bits.GNU_io := io.in.bits.GNU_io
    lsu.io.in.bits.Mem_rdata := io.in.bits.Mem_rdata
    io.out.bits.Mem_wraddr  := lsu.io.out.bits.Mem_wraddr
    io.out.bits.Mem_wdata   := lsu.io.out.bits.Mem_wdata
    io.out.bits.MemOp       := lsu.io.out.bits.MemOp
    io.out.bits.MemWr       := lsu.io.out.bits.MemWr
}