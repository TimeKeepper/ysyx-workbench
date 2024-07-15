package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

// riscv cpu register file

class REG_input extends Bundle{
  val csr_raddr  = Input(UInt(12.W))

  val GPR_raddra = Input(UInt(5.W))
  val GPR_raddrb = Input(UInt(5.W))

  val inst_valid = Input(Bool())
  val pc  = Input(UInt(32.W))
  val GPR_wdata = Input(UInt(32.W))
  val GPR_waddr = Input(UInt(5.W))
  val GPR_wen   = Input(Bool())
  val csr_ctr    = Input(CSR_Type)
  val csr_waddra = Input(UInt(12.W))
  val csr_waddrb = Input(UInt(12.W))
  val csr_wdataa = Input(UInt(32.W))
  val csr_wdatab = Input(UInt(32.W))
}

class REG_output extends Bundle{
  val GPR_rdataa = Output(UInt(32.W))
  val GPR_rdatab = Output(UInt(32.W))

  val pc = Output(UInt(32.W))

  val csr_rdata = Output(UInt(32.W))
}

class REG extends Module {
  val io = IO(new Bundle {
    val in = new Bundle{
      val csr_raddr  = Input(UInt(12.W))

      val GPR_raddra = Input(UInt(5.W))
      val GPR_raddrb = Input(UInt(5.W))

      val WBU_io     = Input(new WBU_output)
    }
    val out = new REG_output
  })

  val gpr = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  when(io.in.WBU_io.GPR_wen && io.in.WBU_io.GPR_waddr =/= 0.U && io.in.WBU_io.inst_valid === true.B) {
    gpr(io.in.WBU_io.GPR_waddr) := io.in.WBU_io.GPR_wdata
  }

  io.out.GPR_rdataa := gpr(io.in.GPR_raddra)
  io.out.GPR_rdatab := gpr(io.in.GPR_raddrb)

  val pc = RegInit(UInt(32.W), "h80000000".U)

  when(io.in.WBU_io.inst_valid === true.B){
    pc        := io.in.WBU_io.Next_Pc
  }
  io.out.pc := pc

  // 暂时先实现128个
  val csr = RegInit(VecInit(Seq.fill(128)(0.U(32.W))))
  io.out.csr_rdata := csr((io.in.csr_raddr - "h300".U)(6, 0))

  when((io.in.WBU_io.CSR_ctr === CSR_R1W1 || io.in.WBU_io.CSR_ctr === CSR_R1W2) && io.in.WBU_io.inst_valid === true.B) {
    csr((io.in.WBU_io.CSR_waddra - "h300".U)(6, 0)) := io.in.WBU_io.CSR_wdataa
  }

  when(io.in.WBU_io.CSR_ctr === CSR_R1W2 && io.in.WBU_io.inst_valid === true.B) {
    csr((io.in.WBU_io.CSR_waddrb - "h300".U)(6, 0)) := io.in.WBU_io.CSR_wdatab
  }
}
