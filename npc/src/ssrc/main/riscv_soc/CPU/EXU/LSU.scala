package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._

// riscv load store unit

class LSU extends Module{
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(new Bundle{
            val GNU_io    = Input(new GNU_Output)

            val Mem_rdata = Input(UInt(32.W))
        }))

        val out = Decoupled(new Bundle{
            val EXU_io    = new EXU_output
            val Mem_wraddr = Output(UInt(32.W))
            val Mem_wdata  = Output(UInt(32.W))
            val MemOp      = Output(MemOp_Type)
            val MemWr      = Output(Bool())
        })
    })

    val state = RegInit(s_wait_valid)

    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.in.valid,  s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.out.ready, s_wait_valid, s_wait_ready),
        )
    )

    io.out.valid := state === s_wait_ready
    io.in.ready  := state === s_wait_valid

    val RegWr_cache       = RegInit(false.B)
    val Branch_cache      = RegInit(Bran_NJmp)
    val MemtoReg_cache    = RegInit(false.B)
    val csr_ctr_cache     = RegInit(CSR_N)
    val Imm_cache         = RegInit(0.U(32.W))
    val GPR_Adata_cache   = RegInit(0.U(32.W))
    val GPR_waddr_cache   = RegInit(0.U(5.W))
    val PC_cache          = RegInit(0.U(32.W))

    val Mem_rdata_cache   = RegInit(0.U(32.W))  

    when(io.in.valid && io.in.ready){
        RegWr_cache       := io.in.bits.GNU_io.RegWr
        Branch_cache      := io.in.bits.GNU_io.Branch
        MemtoReg_cache    := io.in.bits.GNU_io.MemtoReg
        csr_ctr_cache     := io.in.bits.GNU_io.csr_ctr
        Imm_cache         := io.in.bits.GNU_io.Imm
        GPR_Adata_cache   := io.in.bits.GNU_io.GPR_Adata
        GPR_waddr_cache   := io.in.bits.GNU_io.GPR_waddr
        PC_cache          := io.in.bits.GNU_io.PC

        Mem_rdata_cache   := io.in.bits.Mem_rdata   
    }

    io.out.bits.Mem_wraddr := io.in.bits.GNU_io.GPR_Adata + io.in.bits.GNU_io.Imm
    io.out.bits.Mem_wdata  := io.in.bits.GNU_io.GPR_Bdata
    io.out.bits.MemOp      := io.in.bits.GNU_io.MemOp
    io.out.bits.MemWr      := io.in.bits.GNU_io.MemWr & (state === s_wait_ready)

    io.out.bits.EXU_io.RegWr        <> RegWr_cache    
    io.out.bits.EXU_io.Branch       <> Branch_cache   
    io.out.bits.EXU_io.MemtoReg     <> MemtoReg_cache 
    io.out.bits.EXU_io.csr_ctr      <> csr_ctr_cache  
    io.out.bits.EXU_io.Imm          <> Imm_cache      
    io.out.bits.EXU_io.GPR_Adata    <> GPR_Adata_cache
    io.out.bits.EXU_io.GPR_waddr    <> GPR_waddr_cache
    io.out.bits.EXU_io.PC           <> PC_cache       
    io.out.bits.EXU_io.CSR          <> 0.U // not used in LSU
    io.out.bits.EXU_io.Result       <> 0.U   
    io.out.bits.EXU_io.Zero         <> 0.U
    io.out.bits.EXU_io.Less         <> 0.U
    io.out.bits.EXU_io.Mem_rdata    <> Mem_rdata_cache

}
