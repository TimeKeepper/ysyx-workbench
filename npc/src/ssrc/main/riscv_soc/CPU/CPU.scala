package riscv_cpu

import chisel3._
import circt.stage.ChiselStage
import chisel3.util._

import config._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import org.chipsalliance.cde.config.{Parameters, Config}
import freechips.rocketchip.system._
import freechips.rocketchip.diplomacy.LazyModule

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

class riscv_CPU(idBits: Int)(implicit p: Parameters) extends LazyModule {
  val mmio = AddressSet.misaligned(0x10000000, 0x1000) ++
             AddressSet.misaligned(0x10002000, 0x10) ++
             AddressSet.misaligned(0x10011000, 0x8) ++
             AddressSet.misaligned(0x21000000, 0x200000) ++
             AddressSet.misaligned(0x10001000, 0x1000) ++
             AddressSet.misaligned(0x30000000, 0x10000000) ++
             AddressSet.misaligned(0x80000000L, 0x400000) ++
             AddressSet.misaligned(0x2000000, 0x10000) ++
             AddressSet.misaligned(0x0f000000, 0x2000) ++
             AddressSet.misaligned(0xa0000000L, 0x2000000)

  ElaborationArtefacts.add("graphml", graphML)
  val LazyIFU = LazyModule(new ysyx_23060198_IFU(idBits = idBits-1))
  val LazyEXU = LazyModule(new ysyx_23060198_EXU(idBits = idBits-1))

  // val xbar_test = AXI4Xbar(maxFlightPerId = 1, awQueueDepth = 1)
  val xbar_test = AXI4Xbar()
  xbar_test := LazyIFU.masterNode
  xbar_test := LazyEXU.masterNode
  
  val beatBytes = 4
  val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
    Seq(AXI4SlaveParameters(
        address       = mmio,
        executable    = true,
        supportsWrite = TransferSizes(1, beatBytes),
        supportsRead  = TransferSizes(1, beatBytes),
        interleavedId = Some(0))
    ),
    beatBytes  = beatBytes)))

  node := xbar_test
  override lazy val module = new Impl
  class Impl extends LazyModuleImp(this) with DontTouch {
    val io = IO(new Bundle {
      val master = AXI4Bundle(CPUAXI4BundleParameters())
      val slave  = Flipped(AXI4Bundle(CPUAXI4BundleParameters()))
      val interrupt = Input(Bool())
    })
    
    val IFU             = LazyIFU.module
    val IDU             = Module(new ysyx_23060198_IDU)
    val EXU             = LazyEXU.module
    val WBU             = Module(new ysyx_23060198_WBU)
    val REG             = Module(new ysyx_23060198_REG) 
    // val AXI_Interconnect = Module(new ysyx_23060198_AXI_Interconnect)

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
    io.master <> node.in(0)._1
    // AXI_Interconnect.io.AXI.r.bits.data := io.master.r.bits.data

    // AXI_Interconnect.io.ls_resq := IFU.io.IFU_2_IDU.valid
    // AXI_Interconnect.io.if_resq := EXU.io.EXU_2_WBU.valid

    // AXI_Interconnect.io.IFU         <> IFU.io.AXI
    // AXI_Interconnect.io.LSU         <> EXU.io.AXI

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
}

import peripheral._
import ram._
class npc(idBits: Int)(implicit p: Parameters) extends LazyModule {

  ElaborationArtefacts.add("graphml", graphML)
  val LazyIFU = LazyModule(new ysyx_23060198_IFU(idBits = idBits))
  val LazyEXU = LazyModule(new ysyx_23060198_EXU(idBits = idBits))

  val xbar = AXI4Xbar()
  xbar := LazyIFU.masterNode
  xbar := LazyEXU.masterNode
  val beatBytes = 4

  val luart = LazyModule(new UART(AddressSet.misaligned(0x10000000, 0x1000)))
  val lclint = LazyModule(new CLINT(AddressSet.misaligned(0xa0000048L, 0x10)))
  val lsram = LazyModule(new SRAM(AddressSet.misaligned(0x80000000L, 0x2000000), 1.U))

  luart.node := xbar
  lclint.node := xbar
  lsram.node := xbar
  override lazy val module = new Impl
  class Impl extends LazyModuleImp(this) with DontTouch {
    
    val IFU             = LazyIFU.module
    val IDU             = Module(new ysyx_23060198_IDU)
    val EXU             = LazyEXU.module
    val WBU             = Module(new ysyx_23060198_WBU)
    val REG             = Module(new ysyx_23060198_REG) 
    // val AXI_Interconnect = Module(new ysyx_23060198_AXI_Interconnect)

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
    // io.master <> node.in(0)._1
    // AXI_Interconnect.io.AXI.r.bits.data := io.master.r.bits.data

    // AXI_Interconnect.io.ls_resq := IFU.io.IFU_2_IDU.valid
    // AXI_Interconnect.io.if_resq := EXU.io.EXU_2_WBU.valid

    // AXI_Interconnect.io.IFU         <> IFU.io.AXI
    // AXI_Interconnect.io.LSU         <> EXU.io.AXI

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

      // val axi_bridge = Module(new AXI_BRIDGE)
      // axi_bridge.io.clock := clock
      // axi_bridge.io.rresp := node.in1(0)._1.r.bits.resp
      // axi_bridge.io.bresp := node.in1(0)._1.b.bits.resp
    }
  }
}

class ysyx_23060198 extends Module {
  implicit val config: Parameters = new Config(new Edge32BitConfig ++ new DefaultRV32Config)
  
  val io = IO(new Bundle {
      val master = AXI4Bundle(CPUAXI4BundleParameters())
      val slave  = Flipped(AXI4Bundle(CPUAXI4BundleParameters()))
      val interrupt = Input(Bool()) 
  })
  val dut = LazyModule(new riscv_CPU(idBits = ChipLinkParam.idBits))
  val mdut = Module(dut.module)

  mdut.dontTouchPorts()
  
  mdut.io.master <> io.master
  io.slave <> mdut.io.slave
  io.interrupt <> mdut.io.interrupt
}

class top extends Module {
  implicit val config: Parameters = new Config(new Edge32BitConfig ++ new DefaultRV32Config)

  val dut = LazyModule(new npc(idBits = ChipLinkParam.idBits))
  val mdut = Module(dut.module)
  mdut.dontTouchPorts()
}
