package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

// riscv cpu register file

class REG_input extends Bundle {
  val GPR_wdata = Input(UInt(32.W))
  val GPR_waddr = Input(UInt(5.W))
  val GPR_wen   = Input(Bool())

  val GPR_raddra = Input(UInt(5.W))
  val GPR_raddrb = Input(UInt(5.W))

  val pc = Input(UInt(32.W))

  val csr_ctr    = Input(CSR_Type)
  val csr_waddra = Input(UInt(12.W))
  val csr_waddrb = Input(UInt(12.W))
  val csr_wdataa = Input(UInt(32.W))
  val csr_wdatab = Input(UInt(32.W))
  val csr_raddr  = Input(UInt(12.W))

  val valid      = Input(Bool())
}

class REG_output extends Bundle {
  val GPR_rdataa = Output(UInt(32.W))
  val GPR_rdatab = Output(UInt(32.W))

  val pc = Output(UInt(32.W))

  val csr_rdata = Output(UInt(32.W))
}

class REG extends Module {
  val io = IO(new Bundle {
    val in  = new REG_input
    val out = new REG_output
  })

  val gpr = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  when(io.in.GPR_wen && (io.in.GPR_waddr =/= 0.U) && io.in.valid) {
    gpr(io.in.GPR_waddr) := io.in.GPR_wdata
  }

  io.out.GPR_rdataa := gpr(io.in.GPR_raddra)
  io.out.GPR_rdatab := gpr(io.in.GPR_raddrb)

  val pc = RegInit(UInt(32.W), "h80000000".U)

  when(io.in.valid) {
    pc        := io.in.pc
  }
  io.out.pc := pc

  // 暂时先实现128个
  val csr = RegInit(VecInit(Seq.fill(128)(0.U(32.W))))
  io.out.csr_rdata := csr((io.in.csr_raddr - "h300".U)(6, 0))

  when((io.in.csr_ctr === CSR_R1W1) || (io.in.csr_ctr === CSR_R1W2) && io.in.valid) {
    csr((io.in.csr_waddra - "h300".U)(6, 0)) := io.in.csr_wdataa
  }

  when((io.in.csr_ctr === CSR_R1W2) && io.in.valid) {
    csr((io.in.csr_waddrb - "h300".U)(6, 0)) := io.in.csr_wdatab
  }
}
