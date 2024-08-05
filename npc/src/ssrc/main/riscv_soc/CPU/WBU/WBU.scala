package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._

// riscv writeback unit

class ysyx_23060198_WBU extends Module {
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(new Bundle{
            val EXU_io = new EXU_output
        }))
        val out = Decoupled(new Bundle{
            val WBU_io = new WBU_output
        })
    })

    val state = RegInit(s_wait_ready)

    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.in.valid,  s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.out.ready, s_wait_valid, s_wait_ready),
        )
    )

    io.out.valid := state === s_wait_ready && !reset.asBool // 这是由于soc外设的行为不确定而做出的改动
    io.in.ready  := state === s_wait_valid

    val bcu = Module(new ysyx_23060198_BCU)    

    bcu.io.Branch   <> io.in.bits.EXU_io.Branch
    bcu.io.Zero     <> io.in.bits.EXU_io.Zero
    bcu.io.Less     <> io.in.bits.EXU_io.Less
    
    val PCAsrc = Wire(UInt(32.W))
    val PCBsrc = Wire(UInt(32.W))

    PCAsrc := MuxLookup(bcu.io.PCAsrc, 0.U)(Seq(
        PCAsrc_Imm -> io.in.bits.EXU_io.Imm,
        PCAsrc_0  -> 0.U,
        PCAsrc_4 -> 4.U,
        PCAsrc_csr -> io.in.bits.EXU_io.CSR,
    ))

    PCBsrc := MuxLookup(bcu.io.PCBsrc, 0.U)(Seq(
        PCBsrc_gpr -> io.in.bits.EXU_io.GPR_Adata,
        PCBsrc_pc  -> io.in.bits.EXU_io.PC,
        PCBsrc_0   -> 0.U,
    ))

    when(io.in.valid && io.in.ready){
        io.out.bits.WBU_io.inst_valid := true.B
    }.otherwise{
        io.out.bits.WBU_io.inst_valid := false.B
    }

    io.out.bits.WBU_io.Next_Pc := PCAsrc + PCBsrc

    io.out.bits.WBU_io.GPR_waddr := io.in.bits.EXU_io.GPR_waddr
    io.out.bits.WBU_io.GPR_wdata := MuxLookup(io.in.bits.EXU_io.MemtoReg, io.in.bits.EXU_io.Result)(Seq(
        Y  -> io.in.bits.EXU_io.Mem_rdata,
        N  -> Mux(io.in.bits.EXU_io.csr_ctr === CSR_N, io.in.bits.EXU_io.Result, io.in.bits.EXU_io.CSR),
    ))
    io.out.bits.WBU_io.GPR_wen <> io.in.bits.EXU_io.RegWr

    io.out.bits.WBU_io.CSR_ctr <> io.in.bits.EXU_io.csr_ctr

    io.out.bits.WBU_io.CSR_waddra := MuxLookup(io.in.bits.EXU_io.csr_ctr, io.in.bits.EXU_io.Imm(11, 0))(Seq(
        CSR_R1W2 -> "h341".U
    ))

    io.out.bits.WBU_io.CSR_waddrb := "h342".U

    io.out.bits.WBU_io.CSR_wdataa := MuxLookup(io.in.bits.EXU_io.csr_ctr, io.in.bits.EXU_io.Result)(Seq(
        CSR_R1W2 -> io.in.bits.EXU_io.PC,
    ))

    io.out.bits.WBU_io.CSR_wdatab := 11.U
}