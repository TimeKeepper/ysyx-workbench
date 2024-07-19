package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._
import Instructions._
// riscv generating number(all meassge ALU and other thing needs) unit

class GNU extends Module{
    val io = IO(new Bundle{
        // Form IFU
        val in          = Flipped(Decoupled(new Bundle{
            val IFU_io     = new IFU_Output
            val PC         = UInt(32.W)
            val GPR_Adata  = UInt(32.W)
            val GPR_Bdata  = UInt(32.W)
        }))

        val out         = Decoupled(new Bundle{
            val GNU_io     = new GNU_Output

            // To Register File
            val CSR_raddr   = Output(UInt(12.W))
        })
    })

    val state = RegInit(s_wait_valid)

    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.in.valid,  s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.out.ready, s_wait_valid, s_wait_ready),
        )
    )

    val idu = Module(new IDU)
    val igu = Module(new IGU)

    io.out.valid := state === s_wait_ready
    io.in.ready  := state === s_wait_valid

    val RegWr_cache     = RegInit(false.B)
    val Branch_cache    = RegInit(Bran_NJmp)
    val MemtoReg_cache  = RegInit(false.B)
    val MemWr_cache     = RegInit(false.B)
    val MemOp_cache     = RegInit(MemOp_1BS)
    val ALUAsrc_cache   = RegInit(ALUAsrc_RS1)
    val ALUBsrc_cache   = RegInit(ALUBSrc_RS2)
    val ALUctr_cache    = RegInit(ALUctr_ADD)
    val csr_ctr_cache   = RegInit(CSR_N)
    val Imm_cache       = RegInit(0.U(32.W))
    val GPR_Adata_cache = RegInit(0.U(32.W))
    val GPR_Bdata_cache = RegInit(0.U(32.W))
    val GPR_waddr_cache = RegInit(0.U(5.W))
    val PC_cache        = RegInit(0.U(32.W))
    val CSR_raddr_cache = RegInit(0.U(12.W))

    when(io.in.valid && io.in.ready){
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
        GPR_waddr_cache := io.in.bits.IFU_io.data(11, 7)
        PC_cache := io.in.bits.PC
        CSR_raddr_cache := MuxLookup(idu.io.csr_ctr, igu.io.imm(11, 0))(Seq(
            CSR_R1W0 -> "h341".U,
            CSR_R1W2 -> "h305".U,
        ))
    }


    idu.io.inst     <> io.in.bits.IFU_io.data

    io.out.bits.GNU_io.RegWr    <> RegWr_cache
    io.out.bits.GNU_io.Branch   <> Branch_cache
    io.out.bits.GNU_io.MemtoReg  <> MemtoReg_cache
    io.out.bits.GNU_io.MemWr    <> MemWr_cache
    io.out.bits.GNU_io.MemOp    <> MemOp_cache
    io.out.bits.GNU_io.ALUAsrc  <> ALUAsrc_cache
    io.out.bits.GNU_io.ALUBsrc  <> ALUBsrc_cache
    io.out.bits.GNU_io.ALUctr   <> ALUctr_cache
    io.out.bits.GNU_io.csr_ctr  <> csr_ctr_cache
    io.out.bits.GNU_io.Imm      <> Imm_cache
    io.out.bits.GNU_io.GPR_Adata <> GPR_Adata_cache
    io.out.bits.GNU_io.GPR_Bdata <> GPR_Bdata_cache
    io.out.bits.GNU_io.GPR_waddr <> GPR_waddr_cache
    io.out.bits.GNU_io.PC       <> PC_cache
    io.out.bits.CSR_raddr    <> CSR_raddr_cache

    igu.io.inst     <> io.in.bits.IFU_io.data
    igu.io.ExtOp    <> idu.io.ExtOp
}