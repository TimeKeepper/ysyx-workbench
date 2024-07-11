package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import Instructions._
// riscv generating number(all meassge ALU and other thing needs) unit

class GNU_input extends Bundle{
    val PC   = Input(UInt(32.W))
    val inst = Input(UInt(32.W))
    val GPR_Adata = Input(UInt(32.W))
    val GPR_Bdata = Input(UInt(32.W))
}

class GNU_output extends Bundle{
    val inst     = Output(UInt(32.W))
    val RegWr    = Output(Bool())
    val Branch   = Output(Bran_Type)
    val MemtoReg = Output(Bool())
    val MemWr    = Output(Bool())
    val MemOp    = Output(MemOp_Type)
    val ALUAsrc  = Output(ALUAsrc_Type)
    val ALUBsrc  = Output(ALUBSrc_Type)
    val ALUctr   = Output(ALUctr_Type)
    val csr_ctr  = Output(CSR_Type)
    val Imm      = Output(UInt(32.W))
    val GPR_Adata = Output(UInt(32.W))
    val GPR_Bdata = Output(UInt(32.W))
    val GPR_waddr = Output(UInt(5.W))
    val PC       = Output(UInt(32.W))
    val CSR_raddr= Output(UInt(12.W))
}

class GNU extends Module{
    val io = IO(new Bundle{
        val in       = Flipped(Decoupled(new GNU_input))
        val out      = Decoupled(new GNU_output)
    })

    val s_wait_valid :: s_wait_ready :: s_busy :: Nil = Enum(3)
    val state = RegInit(s_wait_valid)

    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.in.valid,  s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.out.ready, s_wait_valid, s_wait_ready),
        )
    )

    val inst_cache = RegInit(0.U(32.W))
    val RegWr_cache = RegInit(false.B)
    val Branch_cache = RegInit(Bran_NJmp)
    val MemtoReg_cache = RegInit(false.B)
    val MemWr_cache = RegInit(false.B)
    val MemOp_cache = RegInit(MemOp_1BS)
    val ALUAsrc_cache = RegInit(ALUAsrc_RS1)
    val ALUBsrc_cache = RegInit(ALUBSrc_RS2)
    val ALUctr_cache = RegInit(ALUctr_ADD)
    val csr_ctr_cache = RegInit(CSR_N)
    val Imm_cache = RegInit(0.U(32.W))
    val GPR_Adata_cache = RegInit(0.U(32.W))
    val GPR_Bdata_cache = RegInit(0.U(32.W))
    val GPR_waddr_cache = RegInit(0.U(5.W))
    val PC_cache = RegInit(0.U(32.W))

    io.out.valid := state === s_wait_ready
    io.in.ready  := state === s_wait_valid

    val idu = Module(new IDU)
    val igu = Module(new IGU)

    when(io.in.valid && io.in.ready){
        inst_cache := io.in.bits.inst
        RegWr_cache := idu.io.RegWr
        Branch_cache := idu.io.Branch
        MemtoReg_cache := idu.io.MemtoReg
        MemWr_cache := idu.io.MemWr
        MemOp_cache := idu.io.MemOp
        ALUAsrc_cache := idu.io.ALUAsrc
        ALUBsrc_cache := idu.io.ALUBsrc
        ALUctr_cache := idu.io.ALUctr
        csr_ctr_cache := idu.io.csr_ctr
        Imm_cache := igu.io.imm
        GPR_Adata_cache := io.in.bits.GPR_Adata
        GPR_Bdata_cache := io.in.bits.GPR_Bdata
        GPR_waddr_cache := io.in.bits.inst(11, 7)
        PC_cache := io.in.bits.PC
    }


    idu.io.inst     <> io.in.bits.inst

    io.out.bits.inst     <> inst_cache
    io.out.bits.RegWr    <> RegWr_cache
    io.out.bits.Branch   <> Branch_cache
    io.out.bits.MemtoReg  <> MemtoReg_cache
    io.out.bits.MemWr    <> MemWr_cache
    io.out.bits.MemOp    <> MemOp_cache
    io.out.bits.ALUAsrc  <> ALUAsrc_cache
    io.out.bits.ALUBsrc  <> ALUBsrc_cache
    io.out.bits.ALUctr   <> ALUctr_cache
    io.out.bits.csr_ctr  <> csr_ctr_cache
    io.out.bits.Imm      <> Imm_cache
    io.out.bits.GPR_Adata <> GPR_Adata_cache
    io.out.bits.GPR_Bdata <> GPR_Bdata_cache
    io.out.bits.GPR_waddr <> GPR_waddr_cache
    io.out.bits.PC       <> PC_cache
    io.out.bits.CSR_raddr<> MuxLookup(io.out.bits.csr_ctr, io.out.bits.Imm(11, 0))(Seq(
        CSR_R1W0 -> "h341".U,
        CSR_R1W2 -> "h305".U,
    ))

    igu.io.inst     <> io.in.bits.inst
    igu.io.ExtOp    <> idu.io.ExtOp
}