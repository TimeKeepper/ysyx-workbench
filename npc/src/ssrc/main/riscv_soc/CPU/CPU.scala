package riscv_cpu

import chisel3._
import chisel3.util._

import config._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
 
class INST_BRIDGE extends BlackBox with HasBlackBoxInline{
  val io = IO(new Bundle{
    val clock = Input(Clock())
    val valid = Input(Bool())
  })
  setInline("INST_BRIDGE.v",
  """module INST_BRIDGE(
  |  input clock,
  |  input valid
  |);
  | import "DPI-C" function void inst_comp_update();
  | always @(posedge clock) begin
  |     if(valid) begin
  |         inst_comp_update();
  |     end
  | end
  |
  |endmodule
  """.stripMargin)
}

class AXI_BRIDGE extends BlackBox with HasBlackBoxInline{
  val io = IO(new Bundle {
      val clock = Input(Clock())
      val rresp  = Input(UInt(2.W))
      val bresp  = Input(UInt(2.W))
  })
  setInline("AXI_BRIDGE.v",
  """module AXI_BRIDGE(
  |  input clock,
  |  input [1:0] rresp,
  |  input [1:0] bresp
  |);
  |import "DPI-C" function void error_waddr();
  |always @(posedge clock) begin
  |    if((rresp == 2'b11) || (bresp == 2'b11)) begin
  |        error_waddr();
  |    end
  |end
  |endmodule
  """.stripMargin)
}

object CPUAXI4BundleParameters {
  def apply() = AXI4BundleParameters(addrBits = 32, dataBits = 32, idBits = ChipLinkParam.idBits)
}

class ysyx_23060198 extends Module {
  val io = IO(new Bundle {
    val master = new AXI4Bundle(CPUAXI4BundleParameters())
    val slave  = new FIX_AXI_BUS_Slave
    val interrupt = Input(Bool())
  })
  
  val IFU             = Module(new ysyx_23060198_IFU)
  val IDU             = Module(new ysyx_23060198_IDU)
  val EXU             = Module(new ysyx_23060198_EXU)
  val WBU             = Module(new ysyx_23060198_WBU)
  val REG             = Module(new ysyx_23060198_REG) 
  val AXI_Interconnect = Module(new ysyx_23060198_AXI_Interconnect)

  // bus IFU -> IDU
  IFU.io.IFU_2_IDU     <> IDU.io.IFU_2_IDU

  // bus IFU -> REG -> IDU without delay
  IFU.io.IFU_2_REG     <> REG.io.IFU_2_REG
  REG.io.REG_2_IDU     <> IDU.io.REG_2_IDU

  // bus IDU -> EXU
  IDU.io.IDU_2_EXU     <> EXU.io.IDU_2_EXU    

  // bus IDU -> REG -> EXU without delay
  IDU.io.IDU_2_REG     <> REG.io.IDU_2_REG
  REG.io.REG_2_EXU     <> EXU.io.REG_2_EXU   

  // bus EXU -> WBU
  EXU.io.EXU_2_WBU     <> WBU.io.EXU_2_WBU   

  // bus WBU -> IFU
  WBU.io.WBU_2_IFU     <> IFU.io.WBU_2_IFU

  // bus WBU -> REG -> IFU without delay
  WBU.io.WBU_2_REG     <> REG.io.WBU_2_REG
  REG.io.REG_2_IFU     <> IFU.io.REG_2_IFU

  // bus AXI Interconnect
  io.master.aw.ready <> AXI_Interconnect.io.AXI.awaddr.ready
  io.master.aw.valid <> AXI_Interconnect.io.AXI.awaddr.valid
  io.master.aw.bits.addr  <> AXI_Interconnect.io.AXI.awaddr.bits.addr
  io.master.aw.bits.size  <> AXI_Interconnect.io.AXI.awaddr.bits.size
  io.master.aw.bits.id    := 0.U
  io.master.aw.bits.len   := 0.U
  io.master.aw.bits.burst := 0.U
  io.master.aw.bits.lock  := 0.U
  io.master.aw.bits.cache := 0.U
  io.master.aw.bits.prot  := 0.U
  io.master.aw.bits.qos   := 0.U

  io.master.w.ready <> AXI_Interconnect.io.AXI.wdata.ready
  io.master.w.valid <> AXI_Interconnect.io.AXI.wdata.valid
  io.master.w.bits.data  <> AXI_Interconnect.io.AXI.wdata.bits.data
  io.master.w.bits.strb   <> AXI_Interconnect.io.AXI.wdata.bits.strb
  io.master.w.bits.last         := 0.U

  io.master.b.ready <> AXI_Interconnect.io.AXI.bresp.ready
  io.master.b.valid <> AXI_Interconnect.io.AXI.bresp.valid
  io.master.b.bits.resp  <> AXI_Interconnect.io.AXI.bresp.bits.bresp
  // io.master.bid    

  io.master.ar.ready <> AXI_Interconnect.io.AXI.araddr.ready
  io.master.ar.valid <> AXI_Interconnect.io.AXI.araddr.valid
  io.master.ar.bits.addr  <> AXI_Interconnect.io.AXI.araddr.bits.addr
  io.master.ar.bits.size  := AXI_Interconnect.io.AXI.araddr.bits.size
  io.master.ar.bits.id    := 0.U
  io.master.ar.bits.len   := 0.U
  io.master.ar.bits.burst := 0.U
  io.master.ar.bits.lock  := 0.U
  io.master.ar.bits.cache := 0.U
  io.master.ar.bits.prot  := 0.U
  io.master.ar.bits.qos   := 0.U

  io.master.r.ready <> AXI_Interconnect.io.AXI.rdata.ready
  io.master.r.valid <> AXI_Interconnect.io.AXI.rdata.valid
  io.master.r.bits.resp  <> AXI_Interconnect.io.AXI.rdata.bits.resp
  AXI_Interconnect.io.AXI.rdata.bits.data := io.master.r.bits.data

  AXI_Interconnect.io.ls_resq := IFU.io.IFU_2_IDU.valid
  AXI_Interconnect.io.if_resq := EXU.io.EXU_2_WBU.valid

  AXI_Interconnect.io.IFU         <> IFU.io.AXI
  AXI_Interconnect.io.LSU         <> EXU.io.AXI

  io.slave <> DontCare
  io.interrupt <> DontCare

  if(Config.DPIC_on){
    val INST_BRIDGE = Module(new INST_BRIDGE)
    INST_BRIDGE.io.clock := clock

    val comp_cache = RegInit(Bool(), false.B)
    comp_cache := WBU.io.WBU_2_IFU.valid
    when((comp_cache === false.B) && (WBU.io.WBU_2_IFU.valid === true.B)) {
      INST_BRIDGE.io.valid := true.B
    }.otherwise {
      INST_BRIDGE.io.valid := false.B
    }

    val axi_bridge = Module(new AXI_BRIDGE)
    axi_bridge.io.clock := clock
    axi_bridge.io.rresp := io.master.r.bits.resp
    axi_bridge.io.bresp := io.master.b.bits.resp
  }
}
