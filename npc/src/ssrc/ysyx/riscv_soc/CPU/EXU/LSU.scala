package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._
import config._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class LSU_DPIC extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle{
        val LS_begin = Input(Bool())
        val addr = Input(UInt(32.W))
    })
    setInline("LSU_DPIC.v",
    """module LSU_DPIC(
      | input  LS_begin,
      | input  [31:0] addr
      |);
      |import "DPI-C" function void LS_differtest_catch(input int addr);
      |
      |  always @ (posedge LS_begin) begin
      |     LS_differtest_catch(addr);
      |  end
      |endmodule
    """.stripMargin)
}

class LSU_catch extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle{
        val LS = Input(Bool())
        val diff_skip = Input(Bool())
    })
    setInline("LSU_catch.v",
    """module LSU_catch(
    |   input LS,
    |   input diff_skip
    |);
    |  import "DPI-C" function void LSU_catch(input int unsigned diff_skip);
    |  always @(posedge LS) begin
    |       LSU_catch({31'h00000000, diff_skip});
    |  end
    |endmodule
    """.stripMargin)
}

class LSU_PC extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val valid = Input(Bool())
    })
    setInline("LSU_PC.v",
    """module LSU_PC(
    |    input clock,
    |    input valid
    |);
    |  import "DPI-C" function void LSU_finished();
    |  always @(posedge clock) begin
    |    if(valid) begin
    |      LSU_finished();
    |    end
    |  end
    |endmodule
    """.stripMargin)
}

object LS_state extends ChiselEnum{
  val s_wait_valid,
      s_load,
      s_store,
      s_store_1,
      s_wb,
      s_sd
      = Value
}

class LSU extends Module{
    val io = IO(new Bundle{
        val IDU_2_EXU = Flipped(Decoupled(Input(new BUS_IDU_2_EXU)))

        val out = Decoupled(new Bundle{
            val Mem_rdata  = Output(UInt(32.W))
        })
        val AXI = AXI4Bundle(CPUAXI4BundleParameters())
    })
    
    io.AXI.ar.bits.id    := 1.U
    io.AXI.ar.bits.len   := 0.U
    io.AXI.ar.bits.burst := 0.U
    io.AXI.ar.bits.lock  := 0.U
    io.AXI.ar.bits.cache := 0.U
    io.AXI.ar.bits.prot  := 0.U
    io.AXI.ar.bits.qos   := 0.U
    io.AXI.aw.bits.id    := 1.U
    io.AXI.aw.bits.len   := 0.U
    io.AXI.aw.bits.burst := 0.U
    io.AXI.aw.bits.lock  := 0.U
    io.AXI.aw.bits.cache := 0.U
    io.AXI.aw.bits.prot  := 0.U
    io.AXI.aw.bits.qos   := 0.U
    io.AXI.w.bits.last   := 1.U

    val state = RegInit(LS_state.s_wait_valid)

    state := MuxLookup(state, LS_state.s_wait_valid)(
        Seq(
            LS_state.s_wait_valid -> Mux(io.IDU_2_EXU.valid, MuxLookup(io.IDU_2_EXU.bits.EXUctr, LS_state.s_wait_valid)(Seq(
                EXUctr_TypeEnum.EXUctr_LD -> LS_state.s_load,
                EXUctr_TypeEnum.EXUctr_ST -> LS_state.s_store
            )), LS_state.s_wait_valid),

            LS_state.s_load -> Mux(io.AXI.ar.ready, LS_state.s_wb, LS_state.s_load),

            LS_state.s_store -> Mux(io.AXI.aw.ready, Mux(io.AXI.w.ready, LS_state.s_sd, LS_state.s_store_1), LS_state.s_store),
            LS_state.s_store_1 -> Mux(io.AXI.w.ready, LS_state.s_sd, LS_state.s_store_1),

            LS_state.s_wb -> Mux(io.out.fire, LS_state.s_wait_valid, LS_state.s_wb),

            LS_state.s_sd -> Mux(io.out.fire, LS_state.s_wait_valid, LS_state.s_sd)
        )
    )

    io.IDU_2_EXU.ready := state === LS_state.s_wait_valid

    val addr = WireDefault(io.IDU_2_EXU.bits.EXU_A + io.IDU_2_EXU.bits.Imm)
    val data = WireDefault((io.IDU_2_EXU.bits.EXU_B << (addr(1,0) << 3.U))(31, 0))

    io.AXI.ar.bits.addr  := RegEnable(addr, io.IDU_2_EXU.fire)
    io.AXI.aw.bits.addr  := RegEnable(addr, io.IDU_2_EXU.fire)
    io.AXI.w.bits.data   := RegEnable(data, io.IDU_2_EXU.fire)

    io.AXI.ar.valid := state === LS_state.s_load
    io.AXI.r.ready := Mux(state === LS_state.s_wb, io.out.ready, false.B)

    io.AXI.aw.valid := state === LS_state.s_store
    io.AXI.w.valid := state === LS_state.s_store || state === LS_state.s_store_1

    io.AXI.b.ready := state === LS_state.s_sd
    
    io.out.valid := MuxLookup(state, false.B)(
        Seq(
            LS_state.s_wb -> io.AXI.r.valid,
            LS_state.s_sd -> io.AXI.b.valid
        )
    )
    
    when(io.IDU_2_EXU.bits.MemOp === MemOp_TypeEnum.MemOp_1BU || io.IDU_2_EXU.bits.MemOp === MemOp_TypeEnum.MemOp_1BS){
        io.AXI.w.bits.strb   := MuxLookup(io.AXI.aw.bits.addr(1,0), "b0001".U)(Seq(
            "b00".U -> "b0001".U,
            "b01".U -> "b0010".U,
            "b10".U -> "b0100".U,
            "b11".U -> "b1000".U,
        ))
    }.elsewhen(io.IDU_2_EXU.bits.MemOp === MemOp_TypeEnum.MemOp_2BU || io.IDU_2_EXU.bits.MemOp === MemOp_TypeEnum.MemOp_2BS){
        io.AXI.w.bits.strb   := MuxLookup(io.AXI.aw.bits.addr(1,0), "b0011".U)(Seq(
            "b00".U -> "b0011".U,
            "b01".U -> "b0110".U,
            "b10".U -> "b1100".U,
        ))
    }.otherwise{
        io.AXI.w.bits.strb   := "b1111".U
    }
    
    io.AXI.aw.bits.size  := MuxLookup(io.IDU_2_EXU.bits.MemOp, 0.U)(Seq(
        MemOp_TypeEnum.MemOp_1BU -> 0.U,
        MemOp_TypeEnum.MemOp_1BS -> 0.U,
        MemOp_TypeEnum.MemOp_2BU -> 1.U,
        MemOp_TypeEnum.MemOp_2BS -> 1.U,
        MemOp_TypeEnum.MemOp_4BU -> 2.U,
    ))
    io.AXI.ar.bits.size  := MuxLookup(io.IDU_2_EXU.bits.MemOp, 0.U)(Seq(
        MemOp_TypeEnum.MemOp_1BU -> 0.U,
        MemOp_TypeEnum.MemOp_1BS -> 0.U,
        MemOp_TypeEnum.MemOp_2BU -> 1.U,
        MemOp_TypeEnum.MemOp_2BS -> 1.U,
        MemOp_TypeEnum.MemOp_4BU -> 2.U,
    ))

    val AXI_rdata = Wire(UInt(32.W))
    AXI_rdata := (io.AXI.r.bits.data >> (io.AXI.ar.bits.addr(1,0) << 3.U))(31, 0)

    val mem_rd = Wire(Bits(32.W))

    mem_rd := MuxLookup(io.IDU_2_EXU.bits.MemOp, 0.U)(Seq(
        MemOp_TypeEnum.MemOp_1BU -> Cat(Fill(24, 0.U), AXI_rdata(7,0)),
        MemOp_TypeEnum.MemOp_1BS -> Cat(Fill(24, AXI_rdata(7)), AXI_rdata(7,0)),
        MemOp_TypeEnum.MemOp_2BU -> Cat(Fill(16, 0.U), AXI_rdata(15,0)),
        MemOp_TypeEnum.MemOp_2BS -> Cat(Fill(16, AXI_rdata(15)), AXI_rdata(15,0)),
        MemOp_TypeEnum.MemOp_4BU -> AXI_rdata(31,0).asUInt,
    ))

    io.out.bits.Mem_rdata := mem_rd

    if(Config.DPIC_on){
        val LS_DPIC = Module(new LSU_DPIC)
        LS_DPIC.io.LS_begin  := io.AXI.ar.valid || io.AXI.aw.valid
        LS_DPIC.io.addr      := io.IDU_2_EXU.bits.EXU_A + io.IDU_2_EXU.bits.Imm

        val LSU_PC = Module(new LSU_PC)
        LSU_PC.io.clock := clock
        LSU_PC.io.valid := io.out.fire && !reset.asBool
    }

    if(Config.Simulate){
        val diff_mis_map =  AddressSet.misaligned(0x10000000, 0x1000) ++
                            AddressSet.misaligned(0x10002000, 0x10) ++
                            AddressSet.misaligned(0x10011000, 0x8) ++
                            AddressSet.misaligned(0x02000000L, 0x10000)
        val Catch = Module(new LSU_catch)
        Catch.io.LS := io.AXI.ar.valid || io.AXI.aw.valid
        Catch.io.diff_skip := diff_mis_map.map(_.contains(RegEnable(addr, io.IDU_2_EXU.fire))).reduce(_ || _)
    }
}
