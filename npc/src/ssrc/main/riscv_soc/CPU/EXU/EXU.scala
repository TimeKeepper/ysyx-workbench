package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._

// riscv excution unit

class ysyx_23060198_EXU extends Module {
    val io = IO(new Bundle{
        // From CSR
        val GNU_2_EXU = Flipped(Decoupled(Input(new BUS_GNU_2_EXU)))
        val CSR       = Input(UInt(32.W))

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

    val alu = Module(new ysyx_23060198_ALU)
    val lsu = Module(new ysyx_23060198_LSU)

    when(io.GNU_2_EXU.bits.GNU_2_EXU.MemWr || io.GNU_2_EXU.bits.GNU_2_EXU.MemtoReg){
        alu.io.GNU_2_EXU.valid := false.B
        alu.io.out.ready := false.B
        io.GNU_2_EXU.ready <> lsu.io.GNU_2_EXU.ready
        io.GNU_2_EXU.valid <> lsu.io.GNU_2_EXU.valid
        io.out.ready <> lsu.io.out.ready
        io.out.valid <> lsu.io.out.valid
    }.otherwise{
        lsu.io.GNU_2_EXU.valid := false.B
        lsu.io.out.ready := false.B
        io.GNU_2_EXU.ready <> alu.io.GNU_2_EXU.ready
        io.GNU_2_EXU.valid <> alu.io.GNU_2_EXU.valid
        io.out.ready <> alu.io.out.ready
        io.out.valid <> alu.io.out.valid
    }

    when(io.GNU_2_EXU.valid && io.GNU_2_EXU.ready){
        RegWr_cache       := io.GNU_2_EXU.bits.GNU_2_EXU.RegWr
        Branch_cache      := io.GNU_2_EXU.bits.GNU_2_EXU.Branch
        MemtoReg_cache    := io.GNU_2_EXU.bits.GNU_2_EXU.MemtoReg
        csr_ctr_cache     := io.GNU_2_EXU.bits.GNU_2_EXU.csr_ctr
        Imm_cache         := io.GNU_2_EXU.bits.GNU_2_EXU.Imm
        GPR_Adata_cache   := io.GNU_2_EXU.bits.GNU_2_EXU.GPR_Adata
        GPR_waddr_cache   := io.GNU_2_EXU.bits.GNU_2_EXU.GPR_waddr
        PC_cache          := io.GNU_2_EXU.bits.GNU_2_EXU.PC 

        CSR_cache         := io.GNU_2_EXU.CSR
    }

    alu.io.GNU_2_EXU.bits.GNU_2_EXU := io.GNU_2_EXU.bits.GNU_2_EXU
    alu.io.GNU_2_EXU.CSR         := io.GNU_2_EXU.CSR

    lsu.io.GNU_2_EXU.bits.GNU_2_EXU := io.GNU_2_EXU.bits.GNU_2_EXU
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