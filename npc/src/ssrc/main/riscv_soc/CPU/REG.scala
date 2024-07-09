package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

// riscv cpu register file

class REG extends Module {
  val io = IO(new Bundle {
    val inst_valid = Input(Bool())

    val GPR_wdata = Input(UInt(32.W))
    val GPR_waddr = Input(UInt(5.W))
    val GPR_wen   = Input(Bool())

    val GPR_raddra = Input(UInt(5.W))
    val GPR_raddrb = Input(UInt(5.W))
    val GPR_rdataa = Output(UInt(32.W))
    val GPR_rdatab = Output(UInt(32.W))

    val pc_in  = Input(UInt(32.W))
    val pc_out = Output(UInt(32.W))

    val csr_ctr    = Input(CSR_Type)
    val csr_waddra = Input(UInt(12.W))
    val csr_waddrb = Input(UInt(12.W))
    val csr_wdataa = Input(UInt(32.W))
    val csr_wdatab = Input(UInt(32.W))
    val csr_raddr  = Input(UInt(12.W))
    val csr_rdata  = Output(UInt(32.W))
  })

  val gpr = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  when(io.GPR_wen && io.GPR_waddr =/= 0.U && inst_valid == true.B) {
    gpr(io.GPR_waddr) := io.GPR_wdata
  }

  io.GPR_rdataa := gpr(io.GPR_raddra)
  io.GPR_rdatab := gpr(io.GPR_raddrb)

  val pc = RegInit(UInt(32.W), "h80000000".U)

  when(inst_valid == true.B) {
    pc        := io.pc_in
  }
  io.pc_out := pc

  // 暂时先实现128个
  val csr = RegInit(VecInit(Seq.fill(128)(0.U(32.W))))
  io.csr_rdata := csr((io.csr_raddr - "h300".U)(6, 0))

  when(io.csr_ctr === CSR_R1W1 || io.csr_ctr === CSR_R1W2 && inst_valid == true.B) {
    csr((io.csr_waddra - "h300".U)(6, 0)) := io.csr_wdataa
  }

  when(io.csr_ctr === CSR_R1W2 && inst_valid == true.B) {
    csr((io.csr_waddrb - "h300".U)(6, 0)) := io.csr_wdatab
  }
}
