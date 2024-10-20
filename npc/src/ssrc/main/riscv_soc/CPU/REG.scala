package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import config._

class REG_BRIDGE extends BlackBox with HasBlackBoxInline {
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
  setInline("REG_BRIDGE.v",
  """module REG_BRIDGE(
    |    input clock,
    |    input pc_wen,
    |    input csra_wen,
    |    input csrb_wen,
    |    input gpr_wen,
    |    input [31:0] new_pc,
    |    input [11:0] CSR_waddra,
    |    input [11:0] CSR_waddrb,
    |    input [31:0] new_CSRa,
    |    input [31:0] new_CSRb,
    |    input [4:0] GPR_waddr,
    |    input [31:0] new_GPR
    |);
    |import "DPI-C" function void cpu_value_update(input bit pc_wen, input bit csra_wen, input bit csrb_wen, input bit gpr_wen, input int unsigned new_PC, input int unsigned CSR_waddra, input int unsigned new_CSRa, input int unsigned CSR_waddrb, input int unsigned new_CSRb, input int unsigned GPR_waddr, input int unsigned new_GPR);
    |always @(posedge clock) begin
    |    cpu_value_update(pc_wen, csra_wen, csrb_wen, gpr_wen, new_pc, {20'h00000, CSR_waddra}, new_CSRa, {20'h00000, CSR_waddrb}, new_CSRb, {27'h0000000, GPR_waddr}, new_GPR);
    |end
    |endmodule
  """.stripMargin)
}

// riscv cpu register file

class REG_output extends Bundle{
  val GPR_rdataa = Output(UInt(32.W))
  val GPR_rdatab = Output(UInt(32.W))

  val pc = Output(UInt(32.W))

  val csr_rdata = Output(UInt(32.W))
}

class ysyx_23060198_REG extends Module {
  val io = IO(new Bundle {
    val REG_2_IFU = Output(new BUS_REG_2_IFU)
    val IFU_2_REG = Input(new BUS_IFU_2_REG)
    val REG_2_IDU = Output(new BUS_REG_2_IDU)
    val IDU_2_REG = Input(new BUS_IDU_2_REG)
    val REG_2_EXU = Output(new BUS_REG_2_EXU)
    val WBU_2_REG = Input(new BUS_WBU_2_REG)
  })

  val csra_wen = (io.WBU_2_REG.CSR_ctr === CSR_TypeEnum.CSR_R1W1 || io.WBU_2_REG.CSR_ctr === CSR_TypeEnum.CSR_R1W2) && io.WBU_2_REG.inst_valid === true.B
  val csrb_wen = io.WBU_2_REG.CSR_ctr === CSR_TypeEnum.CSR_R1W2 && io.WBU_2_REG.inst_valid === true.B
  val gpr_wen = io.WBU_2_REG.GPR_waddr =/= 0.U && io.WBU_2_REG.inst_valid === true.B

  val gpr = RegInit(VecInit(Seq.fill(15)(0.U(32.W))))

  when(gpr_wen) {
    gpr((io.WBU_2_REG.GPR_waddr - 1.U)(3, 0)) := io.WBU_2_REG.GPR_wdata
  }

  when(io.IFU_2_REG.GPR_Aaddr =/= 0.U){
    io.REG_2_IDU.GPR_Adata := gpr((io.IFU_2_REG.GPR_Aaddr - 1.U)(3, 0))
  }.otherwise{
    io.REG_2_IDU.GPR_Adata := 0.U
  }

  when(io.IFU_2_REG.GPR_Baddr =/= 0.U){
    io.REG_2_IDU.GPR_Bdata := gpr((io.IFU_2_REG.GPR_Baddr - 1.U)(3, 0))
  }.otherwise{
    io.REG_2_IDU.GPR_Bdata := 0.U
  }

  val pc = RegEnable(io.WBU_2_REG.Next_Pc, main_val.Reset_Vector, io.WBU_2_REG.inst_valid)

  io.REG_2_IDU.PC := pc
  io.REG_2_IFU.Next_PC := pc

  // CSR
  def ADDR_MSTATUS = "h300".U
  def ADDR_MTEVC   = "h305".U
  def ADDR_MSCRATCH= "h340".U
  def ADDR_MEPC    = "h341".U
  def ADDR_MCAUSE  = "h342".U

  def ADDR_MVENDORID = "hF11".U
  def ADDR_MARCHID   = "hF12".U

  val mstatus, mtevc, mepc, mcause, mscratch = RegInit(0.U(32.W))

  io.REG_2_IDU.CSR_rdata := MuxLookup(io.IDU_2_REG.CSR_raddr, 0.U(32.W))(Seq(
    ADDR_MSTATUS   -> mstatus,
    ADDR_MTEVC     -> mtevc,
    ADDR_MSCRATCH  -> mscratch,
    ADDR_MEPC      -> mepc,
    ADDR_MCAUSE    -> mcause,
    ADDR_MVENDORID -> "h79737978".U(32.W), // ysyx
    ADDR_MARCHID   -> "d23060198".U(32.W)  // my id 
  ))

  when(csra_wen) {
    when(io.WBU_2_REG.CSR_waddra === ADDR_MSTATUS){
      mstatus := io.WBU_2_REG.CSR_wdataa
    }.elsewhen(io.WBU_2_REG.CSR_waddra === ADDR_MTEVC){
      mtevc := io.WBU_2_REG.CSR_wdataa
    }.elsewhen(io.WBU_2_REG.CSR_waddra === ADDR_MSCRATCH){
      mscratch := io.WBU_2_REG.CSR_wdataa
    }.elsewhen(io.WBU_2_REG.CSR_waddra === ADDR_MEPC){
      mepc := io.WBU_2_REG.CSR_wdataa
    }.elsewhen(io.WBU_2_REG.CSR_waddra === ADDR_MCAUSE){
      mcause := io.WBU_2_REG.CSR_wdataa
    }
    // csr((io.WBU_2_REG.CSR_waddra - "h300".U)(6, 0)) := io.WBU_2_REG.CSR_wdataa
  }

  when(csrb_wen) {
    when(io.WBU_2_REG.CSR_waddrb === ADDR_MSTATUS){
      mstatus := io.WBU_2_REG.CSR_wdatab
    }.elsewhen(io.WBU_2_REG.CSR_waddrb === ADDR_MTEVC){
      mtevc := io.WBU_2_REG.CSR_wdatab
    }.elsewhen(io.WBU_2_REG.CSR_waddrb === ADDR_MSCRATCH){
      mscratch := io.WBU_2_REG.CSR_wdatab
    }.elsewhen(io.WBU_2_REG.CSR_waddrb === ADDR_MEPC){
      mepc := io.WBU_2_REG.CSR_wdatab
    }.elsewhen(io.WBU_2_REG.CSR_waddrb === ADDR_MCAUSE){
      mcause := io.WBU_2_REG.CSR_wdatab
    }
    // csr((io.WBU_2_REG.CSR_waddrb - "h300".U)(6, 0)) := io.WBU_2_REG.CSR_wdatab
  }
  
  if (Config.DPIC_on){
    val bridge = Module(new REG_BRIDGE)

    bridge.io.clock := clock
    bridge.io.pc_wen := io.WBU_2_REG.inst_valid
    bridge.io.csra_wen := csra_wen
    bridge.io.csrb_wen := csrb_wen
    bridge.io.gpr_wen := gpr_wen
    bridge.io.new_pc := io.WBU_2_REG.Next_Pc
    bridge.io.CSR_waddra := io.WBU_2_REG.CSR_waddra
    bridge.io.CSR_waddrb := io.WBU_2_REG.CSR_waddrb
    bridge.io.new_CSRa := io.WBU_2_REG.CSR_wdataa
    bridge.io.new_CSRb := io.WBU_2_REG.CSR_wdatab
    bridge.io.GPR_waddr := io.WBU_2_REG.GPR_waddr
    bridge.io.new_GPR := io.WBU_2_REG.GPR_wdata
  }
}
