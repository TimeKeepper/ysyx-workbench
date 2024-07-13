package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._

// riscv load store unit

class LSU_input extends Bundle{
    val RegWr       = Input(Bool())
    val Branch      = Input(Bran_Type)
    val MemtoReg    = Input(Bool())
    val MemWr       = Input(Bool())
    val MemOp       = Input(MemOp_Type)
    val csr_ctr     = Input(CSR_Type)
    val Imm         = Input(UInt(32.W))
    val GPR_Adata   = Input(UInt(32.W))
    val GPR_Bdata   = Input(UInt(32.W))
    val GPR_waddr   = Input(UInt(5.W))
    val PC          = Input(UInt(32.W))
    val CSR         = Input(UInt(32.W))
    val Result      = Input(UInt(32.W))
    val Zero        = Input(Bool())
    val Less        = Input(Bool())

    val Mem_rdata   = Input(UInt(32.W))
}

class LSU_output extends Bundle{
    val RegWr       = Output(Bool())
    val Branch      = Output(Bran_Type)
    val MemtoReg    = Output(Bool())
    val csr_ctr     = Output(CSR_Type)
    val Imm         = Output(UInt(32.W))
    val GPR_Adata   = Output(UInt(32.W))
    val GPR_waddr   = Output(UInt(5.W))
    val PC          = Output(UInt(32.W))
    val CSR         = Output(UInt(32.W))
    val Result      = Output(UInt(32.W))
    val Zero        = Output(Bool())
    val Less        = Output(Bool())
    val Mem_rdata   = Output(UInt(32.W))

    val Mem_wraddr = Output(UInt(32.W))
    val Mem_wdata  = Output(UInt(32.W))
    val MemOp      = Output(MemOp_Type)
    val MemWr      = Output(Bool())
}

class LSU extends Module{
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(new LSU_input))
        val out = Decoupled(new LSU_output)
    })

    // val s_wait_valid :: s_wait_ready :: s_busy :: Nil = Enum(3)
    val state = RegInit(s_wait_valid)

    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.in.valid,  s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.out.ready, s_wait_valid, s_wait_ready),
        )
    )

    io.out.valid := state === s_wait_ready
    io.in.ready  := state === s_wait_valid

    val RegWr_cache     = RegInit(false.B)
    val Branch_cache    = RegInit(Bran_NJmp)
    val MemtoReg_cache  = RegInit(false.B)
    val csr_ctr_cache   = RegInit(CSR_N)
    val Imm_cache       = RegInit(0.U(32.W))
    val GPR_Adata_cache = RegInit(0.U(32.W))
    val GPR_waddr_cache = RegInit(0.U(5.W))
    val PC_cache        = RegInit(0.U(32.W))
    val CSR_cache       = RegInit(0.U(32.W))
    val Result_cache    = RegInit(0.U(32.W))  
    val Zero_cache      = RegInit(false.B)  
    val Less_cache      = RegInit(false.B)  
    val Mem_rdata_cache = RegInit(0.U(32.W))  

    when(io.in.valid && io.in.ready){
        RegWr_cache     := io.in.bits.RegWr     
        Branch_cache    := io.in.bits.Branch    
        MemtoReg_cache  := io.in.bits.MemtoReg  
        csr_ctr_cache   := io.in.bits.csr_ctr   
        Imm_cache       := io.in.bits.Imm       
        GPR_Adata_cache := io.in.bits.GPR_Adata 
        GPR_waddr_cache := io.in.bits.GPR_waddr 
        PC_cache        := io.in.bits.PC        
        CSR_cache       := io.in.bits.CSR       
        Result_cache    := io.in.bits.Result    
        Zero_cache      := io.in.bits.Zero      
        Less_cache      := io.in.bits.Less   
        Mem_rdata_cache := io.in.bits.Mem_rdata   
    }

    io.out.bits.Mem_wraddr   := io.in.bits.Result
    io.out.bits.Mem_wdata    := io.in.bits.GPR_Bdata
    io.out.bits.MemOp        := io.in.bits.MemOp
    io.out.bits.MemWr        := io.in.bits.MemWr & (state === s_wait_ready)

    io.out.bits.RegWr        <> RegWr_cache     
    io.out.bits.Branch       <> Branch_cache    
    io.out.bits.MemtoReg     <> MemtoReg_cache  
    io.out.bits.csr_ctr      <> csr_ctr_cache   
    io.out.bits.Imm          <> Imm_cache       
    io.out.bits.GPR_Adata    <> GPR_Adata_cache 
    io.out.bits.GPR_waddr    <> GPR_waddr_cache 
    io.out.bits.PC           <> PC_cache        
    io.out.bits.CSR          <> CSR_cache       
    io.out.bits.Result       <> Result_cache    
    io.out.bits.Zero         <> Zero_cache      
    io.out.bits.Less         <> Less_cache      
    io.out.bits.Mem_rdata    <> Mem_rdata_cache 
}