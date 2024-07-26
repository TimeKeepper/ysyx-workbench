package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._

class reg_bridge extends BlackBox{
  val io = IO(new Bundle{
    val clock = Input(Clock())
    val pc_wen = Input(Bool())
    val csra_wen = Input(Bool())
    val csrb_wen = Input(Bool())
    val gpr_wen = Input(Bool())
    val new_pc = Input(UInt(32.W))
    val CSR_waddra = Input(UInt(12.W))
    val CSR_waddrb = Input(UInt(12.W))
    val new_CSRa = Input(UInt(32.W))
    val new_CSRb = Input(UInt(32.W))
    val GPR_waddr = Input(UInt(5.W))
    val new_GPR = Input(UInt(32.W))
  })
}

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

class ysyx_23060198_REG extends Module {
  val io = IO(new Bundle {
    val in = new Bundle{
      val csr_raddr  = Input(UInt(12.W))

      val GPR_raddra = Input(UInt(5.W))
      val GPR_raddrb = Input(UInt(5.W))

      val WBU_io     = Input(new WBU_output)
    }
    val out = new REG_output
  })

  val pc_wen = io.in.WBU_io.inst_valid === true.B
  val csra_wen = (io.in.WBU_io.CSR_ctr === CSR_R1W1 || io.in.WBU_io.CSR_ctr === CSR_R1W2) && io.in.WBU_io.inst_valid === true.B
  val csrb_wen = io.in.WBU_io.CSR_ctr === CSR_R1W2 && io.in.WBU_io.inst_valid === true.B
  val gpr_wen = io.in.WBU_io.GPR_wen && io.in.WBU_io.GPR_waddr =/= 0.U && io.in.WBU_io.inst_valid === true.B

  val gpr = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  when(gpr_wen) {
    gpr(io.in.WBU_io.GPR_waddr) := io.in.WBU_io.GPR_wdata
  }

  io.out.GPR_rdataa := gpr(io.in.GPR_raddra)
  io.out.GPR_rdatab := gpr(io.in.GPR_raddrb)

  val pc = RegInit(UInt(32.W), "h80000000".U)

  when(pc_wen){
    pc        := io.in.WBU_io.Next_Pc
  }
  io.out.pc := pc

  // CSR
  def ADDR_MSTATUS = "h300".U
  def ADDR_MTEVC   = "h305".U
  def ADDR_MSCRATCH= "h340".U
  def ADDR_MEPC    = "h341".U
  def ADDR_MCAUSE  = "h342".U

  val mstatus, mtevc, mepc, mcause, mscratch = RegInit(0.U(32.W))
  io.out.csr_rdata := MuxLookup(io.in.csr_raddr, 0.U(32.W))(Seq(
    ADDR_MSTATUS   -> mstatus,
    ADDR_MTEVC     -> mtevc,
    ADDR_MSCRATCH  -> mscratch,
    ADDR_MEPC      -> mepc,
    ADDR_MCAUSE    -> mcause,
  ))

  // val csr = RegInit(VecInit(Seq.fill(128)(0.U(32.W))))
  // io.out.csr_rdata := csr((io.in.csr_raddr - "h300".U)(6, 0))

  when(csra_wen) {
    when(io.in.WBU_io.CSR_waddra === ADDR_MSTATUS){
      mstatus := io.in.WBU_io.CSR_wdataa
    }.elsewhen(io.in.WBU_io.CSR_waddra === ADDR_MTEVC){
      mtevc := io.in.WBU_io.CSR_wdataa
    }.elsewhen(io.in.WBU_io.CSR_waddra === ADDR_MSCRATCH){
      mscratch := io.in.WBU_io.CSR_wdataa
    }.elsewhen(io.in.WBU_io.CSR_waddra === ADDR_MEPC){
      mepc := io.in.WBU_io.CSR_wdataa
    }.elsewhen(io.in.WBU_io.CSR_waddra === ADDR_MCAUSE){
      mcause := io.in.WBU_io.CSR_wdataa
    }
    // csr((io.in.WBU_io.CSR_waddra - "h300".U)(6, 0)) := io.in.WBU_io.CSR_wdataa
  }

  when(csrb_wen) {
    when(io.in.WBU_io.CSR_waddrb === ADDR_MSTATUS){
      mstatus := io.in.WBU_io.CSR_wdatab
    }.elsewhen(io.in.WBU_io.CSR_waddrb === ADDR_MTEVC){
      mtevc := io.in.WBU_io.CSR_wdatab
    }.elsewhen(io.in.WBU_io.CSR_waddrb === ADDR_MSCRATCH){
      mscratch := io.in.WBU_io.CSR_wdatab
    }.elsewhen(io.in.WBU_io.CSR_waddrb === ADDR_MEPC){
      mepc := io.in.WBU_io.CSR_wdatab
    }.elsewhen(io.in.WBU_io.CSR_waddrb === ADDR_MCAUSE){
      mcause := io.in.WBU_io.CSR_wdatab
    }
    // csr((io.in.WBU_io.CSR_waddrb - "h300".U)(6, 0)) := io.in.WBU_io.CSR_wdatab
  }
  
  // 只是为了仿真环境，可以去除
  val bridge = Module(new reg_bridge)

  bridge.io.clock := clock
  bridge.io.pc_wen := pc_wen
  bridge.io.csra_wen := csra_wen
  bridge.io.csrb_wen := csrb_wen
  bridge.io.gpr_wen := gpr_wen
  bridge.io.new_pc := io.in.WBU_io.Next_Pc
  bridge.io.CSR_waddra := io.in.WBU_io.CSR_waddra
  bridge.io.CSR_waddrb := io.in.WBU_io.CSR_waddrb
  bridge.io.new_CSRa := io.in.WBU_io.CSR_wdataa
  bridge.io.new_CSRb := io.in.WBU_io.CSR_wdatab
  bridge.io.GPR_waddr := io.in.WBU_io.GPR_waddr
  bridge.io.new_GPR := io.in.WBU_io.GPR_wdata
}
