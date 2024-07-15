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
    val MemWr_cache       = RegInit(false.B)
    val MemOp_cache       = RegInit(MemOp_1BS)
    val csr_ctr_cache     = RegInit(CSR_N)
    val Imm_cache         = RegInit(0.U(32.W))
    val GPR_Adata_cache   = RegInit(0.U(32.W))
    val GPR_Bdata_cache   = RegInit(0.U(32.W))
    val GPR_waddr_cache   = RegInit(0.U(5.W))
    val PC_cache          = RegInit(0.U(32.W))
    val CSR_cache         = RegInit(0.U(32.W))
    val Result_cache      = RegInit(0.U(32.W))
    val Zero_cache        = RegInit(false.B)
    val Less_cache        = RegInit(false.B)

    val alu = Module(new ALU)

    when(io.in.valid && io.in.ready){
        RegWr_cache       := io.in.bits.GNU_io.RegWr
        Branch_cache      := io.in.bits.GNU_io.Branch
        MemtoReg_cache    := io.in.bits.GNU_io.MemtoReg
        MemWr_cache       := io.in.bits.GNU_io.MemWr
        MemOp_cache       := io.in.bits.GNU_io.MemOp
        csr_ctr_cache     := io.in.bits.GNU_io.csr_ctr
        Imm_cache         := io.in.bits.GNU_io.Imm
        GPR_Adata_cache   := io.in.bits.GNU_io.GPR_Adata
        GPR_Bdata_cache   := io.in.bits.GNU_io.GPR_Bdata
        GPR_waddr_cache   := io.in.bits.GNU_io.GPR_waddr
        PC_cache          := io.in.bits.GNU_io.PC
        Result_cache      := alu.io.ALUout 
        Zero_cache        := alu.io.Zero   
        Less_cache        := alu.io.Less  

        CSR_cache         := io.in.bits.CSR
    }

    io.out.bits.EXU_io.RegWr        <> RegWr_cache    
    io.out.bits.EXU_io.Branch       <> Branch_cache   
    io.out.bits.EXU_io.MemtoReg     <> MemtoReg_cache 
    io.out.bits.EXU_io.MemWr        <> MemWr_cache    
    io.out.bits.EXU_io.MemOp        <> MemOp_cache    
    io.out.bits.EXU_io.csr_ctr      <> csr_ctr_cache  
    io.out.bits.EXU_io.Imm          <> Imm_cache      
    io.out.bits.EXU_io.GPR_Adata    <> GPR_Adata_cache
    io.out.bits.EXU_io.GPR_Bdata    <> GPR_Bdata_cache
    io.out.bits.EXU_io.GPR_waddr    <> GPR_waddr_cache
    io.out.bits.EXU_io.PC           <> PC_cache       
    io.out.bits.EXU_io.CSR          <> CSR_cache      
    io.out.bits.EXU_io.Result        <> Result_cache 
    io.out.bits.EXU_io.Zero          <> Zero_cache   
    io.out.bits.EXU_io.Less          <> Less_cache   

    alu.io.ALUctr <> io.in.bits.GNU_io.ALUctr

    alu.io.src_A := MuxLookup(io.in.bits.GNU_io.ALUAsrc, 0.U)(Seq(
        ALUAsrc_RS1 -> io.in.bits.GNU_io.GPR_Adata,
        ALUAsrc_PC  -> io.in.bits.GNU_io.PC,
        ALUAsrc_CSR -> io.in.bits.CSR,
    ))

    alu.io.src_B := MuxLookup(io.in.bits.GNU_io.ALUBsrc, 0.U)(Seq(
        ALUBSrc_RS1 -> io.in.bits.GNU_io.GPR_Adata,
        ALUBSrc_RS2 -> io.in.bits.GNU_io.GPR_Bdata,
        ALUBSrc_IMM -> io.in.bits.GNU_io.Imm,
        ALUBSrc_4   -> 4.U,
    ))
}