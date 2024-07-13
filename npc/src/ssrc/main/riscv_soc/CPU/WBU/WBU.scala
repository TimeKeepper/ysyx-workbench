package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._

// riscv writeback unit

class WBU_input extends Bundle{
    val RegWr       = Input(Bool())
    val Branch      = Input(Bran_Type)
    val MemtoReg    = Input(Bool())
    val csr_ctr     = Input(CSR_Type)
    val Imm         = Input(UInt(32.W))
    val GPR_Adata   = Input(UInt(32.W))
    val GPR_waddr   = Input(UInt(5.W))
    val PC          = Input(UInt(32.W))
    val CSR         = Input(UInt(32.W))
    val Result      = Input(UInt(32.W))
    val Zero        = Input(Bool())
    val Less        = Input(Bool())
    val Mem_rdata   = Input(UInt(32.W))
}

class WBU_output extends Bundle{
    val inst_valid= Output(Bool())
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
        val in = Flipped(Decoupled(new WBU_input))
        val out = Decoupled(new WBU_output)
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

    val bcu = Module(new BCU)    

    bcu.io.Branch   <> io.in.bits.Branch
    bcu.io.Zero     <> io.in.bits.Zero
    bcu.io.Less     <> io.in.bits.Less
    
    val PCAsrc = Wire(UInt(32.W))
    val PCBsrc = Wire(UInt(32.W))

    PCAsrc := MuxLookup(bcu.io.PCAsrc, 0.U)(Seq(
        PCAsrc_Imm -> io.in.bits.Imm,
        PCAsrc_0  -> 0.U,
        PCAsrc_4 -> 4.U,
        PCAsrc_csr -> io.in.bits.CSR,
    ))

    PCBsrc := MuxLookup(bcu.io.PCBsrc, 0.U)(Seq(
        PCBsrc_gpr -> io.in.bits.GPR_Adata,
        PCBsrc_pc  -> io.in.bits.PC,
        PCBsrc_0   -> 0.U,
    ))

    when(io.in.valid && io.in.ready){
        io.out.bits.inst_valid := true.B
    }.otherwise{
        io.out.bits.inst_valid := false.B
    }

    io.out.bits.Next_Pc := PCAsrc + PCBsrc

    io.out.bits.GPR_waddr := io.in.bits.GPR_waddr
    io.out.bits.GPR_wdata := MuxLookup(io.in.bits.MemtoReg, io.in.bits.Result)(Seq(
        Y  -> io.in.bits.Mem_rdata,
        N  -> Mux(io.in.bits.csr_ctr === CSR_N, io.in.bits.Result, io.in.bits.CSR),
    ))
    io.out.bits.GPR_wen <> io.in.bits.RegWr

    io.out.bits.CSR_ctr <> io.in.bits.csr_ctr

    io.out.bits.CSR_waddra := MuxLookup(io.in.bits.csr_ctr, io.in.bits.Imm(11, 0))(Seq(
        CSR_R1W2 -> "h341".U
    ))

    io.out.bits.CSR_waddrb := "h342".U

    io.out.bits.CSR_wdataa := MuxLookup(io.in.bits.csr_ctr, io.in.bits.Result)(Seq(
        CSR_R1W2 -> io.in.bits.PC,
    ))

    io.out.bits.CSR_wdatab := 11.U
}