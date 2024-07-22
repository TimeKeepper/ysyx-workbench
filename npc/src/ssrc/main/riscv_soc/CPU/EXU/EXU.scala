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

            // Form Register File
            val CSR       = Input(UInt(32.W))
        }))

        val out = Decoupled(new Bundle{
            val EXU_io    = new EXU_output
        })
        val AXI = new AXI_Master
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

    val alu = Module(new ALU)
    val lsu = Module(new LSU)

    when(io.in.bits.GNU_io.MemWr || io.in.bits.GNU_io.MemtoReg){
        alu.io.in.valid := false.B
        alu.io.out.ready := false.B
        io.in.ready <> lsu.io.in.ready
        io.in.valid <> lsu.io.in.valid
        io.out.ready <> lsu.io.out.ready
        io.out.valid <> lsu.io.out.valid
    }.otherwise{
        lsu.io.in.valid := false.B
        lsu.io.out.ready := false.B
        io.in.ready <> alu.io.in.ready
        io.in.valid <> alu.io.in.valid
        io.out.ready <> alu.io.out.ready
        io.out.valid <> alu.io.out.valid
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

        CSR_cache         := io.in.bits.CSR
    }

    alu.io.in.bits.GNU_io := io.in.bits.GNU_io
    alu.io.in.bits.CSR    := io.in.bits.CSR

    lsu.io.in.bits.GNU_io := io.in.bits.GNU_io
    lsu.io.AXI <> io.AXI


    io.out.bits.EXU_io.RegWr        <> RegWr_cache    
    io.out.bits.EXU_io.Branch       <> Branch_cache   
    io.out.bits.EXU_io.MemtoReg     <> MemtoReg_cache 
    io.out.bits.EXU_io.csr_ctr      <> csr_ctr_cache  
    io.out.bits.EXU_io.Imm          <> Imm_cache      
    io.out.bits.EXU_io.GPR_Adata    <> GPR_Adata_cache
    io.out.bits.EXU_io.GPR_waddr    <> GPR_waddr_cache
    io.out.bits.EXU_io.PC           <> PC_cache   
    io.out.bits.EXU_io.CSR          <> CSR_cache    

    io.out.bits.EXU_io.Result       <> alu.io.out.bits.Result
    io.out.bits.EXU_io.Zero         <> alu.io.out.bits.Zero
    io.out.bits.EXU_io.Less         <> alu.io.out.bits.Less

    io.out.bits.EXU_io.Mem_rdata    <> lsu.io.out.bits.Mem_rdata
}