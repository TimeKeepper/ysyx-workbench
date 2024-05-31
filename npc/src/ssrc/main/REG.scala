package riscv_cpu

import chisel3._
import chisel3.util._

// riscv cpu register file

class REG extends Module {
    val io = IO(new Bundle {
        val wdata = Input(UInt(32.W))
        val waddr = Input(UInt(5.W))
        val wen   = Input(Bool())

        val raddra = Input(UInt(5.W))
        val raddrb = Input(UInt(5.W))
        val rdataa  = Output(UInt(32.W))
        val rdatab  = Output(UInt(32.W))

        val pc_in  = Input(UInt(32.W))
        val pc_out = Output(UInt(32.W))

        val csr_ctr    = Input(UInt(2.W))
        val csr_waddra = Input(UInt(12.W))
        val csr_waddrb = Input(UInt(12.W))
        val csr_wdataa = Input(UInt(32.W))
        val csr_wdatab = Input(UInt(32.W))
        val csr_raddr  = Input(UInt(12.W))
        val csr_rdata  = Output(UInt(32.W))
    })

    val gpr = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

    when(io.wen && io.waddr =/= 0.U) {
        gpr(io.waddr) := io.wdata
    }

    io.rdataa := gpr(io.raddra)
    io.rdatab := gpr(io.raddrb)

    val pc  = RegInit(UInt(32.W), "h80000000".U)

    pc := io.pc_in
    io.pc_out := pc

    // 暂时先实现1024个
    val csr = RegInit(VecInit(Seq.fill(1024)(0.U(32.W)))) 
    io.csr_rdata := csr(io.csr_raddr(9, 0))

    when(io.csr_ctr === 2.U || io.csr_ctr === 3.U) {
        csr(io.csr_waddra(9, 0)) := io.csr_wdataa
    }

    when(io.csr === 3.U) {
        csr(io.csr_waddrb(9, 0)) := io.csr_wdatab
    }
}