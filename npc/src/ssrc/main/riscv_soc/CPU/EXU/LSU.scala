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
            val Mem_rdata  = Output(UInt(32.W))
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

    val Mem_rdata_cache   = RegInit(0.U(32.W))  

    when(io.in.valid && io.in.ready){
        Mem_rdata_cache   := io.in.bits.Mem_rdata   
    }

    io.out.bits.Mem_wraddr := io.in.bits.GNU_io.GPR_Adata + io.in.bits.GNU_io.Imm
    io.out.bits.Mem_wdata  := io.in.bits.GNU_io.GPR_Bdata
    io.out.bits.MemOp      := io.in.bits.GNU_io.MemOp
    io.out.bits.MemWr      := io.in.bits.GNU_io.MemWr & (state === s_wait_ready)

    io.out.bits.Mem_rdata    <> Mem_rdata_cache
}
