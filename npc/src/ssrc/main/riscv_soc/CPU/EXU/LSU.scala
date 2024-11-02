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

// riscv load store unit

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

class ysyx_23060198_LSU extends Module{
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
    io.AXI.w.bits.last  := 1.U

    val s_idle :: s_wait_addr :: s_wait_data :: Nil = Enum(3)

    val state_write = RegInit(s_idle)

    when(io.IDU_2_EXU.bits.EXUctr  === EXUctr_TypeEnum.EXUctr_ST) {
        io.AXI.ar.valid   := false.B
        io.AXI.r.ready    := false.B
        io.AXI.aw.valid   <> io.IDU_2_EXU.valid
        io.AXI.w.valid    := io.IDU_2_EXU.valid
        io.AXI.b.ready    <> io.out.ready
        io.AXI.b.valid    <> io.out.valid

        when(state_write === s_idle) {
            io.IDU_2_EXU.ready := io.AXI.aw.ready && io.AXI.w.ready

            when(io.IDU_2_EXU.valid){
                when(io.AXI.aw.ready && io.AXI.w.ready){
                    state_write := s_idle
                }.elsewhen(io.AXI.aw.ready && !io.AXI.w.ready){
                    state_write := s_wait_data
                }.elsewhen(!io.AXI.aw.ready && io.AXI.w.ready){
                    state_write := s_wait_addr
                }.otherwise{
                    state_write := s_idle
                }
            }
        }.elsewhen(state_write === s_wait_addr){
            io.IDU_2_EXU.ready := io.AXI.aw.ready
            when(io.IDU_2_EXU.valid && io.AXI.aw.ready){
                state_write := s_idle
            }
        }.elsewhen(state_write === s_wait_data){
            io.IDU_2_EXU.ready := io.AXI.w.ready
            when(io.IDU_2_EXU.valid && io.AXI.w.ready){
                state_write := s_idle
            }
        }.otherwise{
            io.IDU_2_EXU.ready := false.B
        }
    }.elsewhen(io.IDU_2_EXU.bits.EXUctr  === EXUctr_TypeEnum.EXUctr_LD) {
        io.AXI.aw.valid   := false.B
        io.AXI.w.valid    := false.B
        io.AXI.b.ready    := false.B
        io.AXI.ar.ready   <> io.IDU_2_EXU.ready
        io.AXI.ar.valid   <> io.IDU_2_EXU.valid
        io.AXI.r.valid    <> io.out.valid
        io.AXI.r.ready    <> io.out.ready
    }.otherwise {
        io.AXI.ar.valid   := false.B
        io.AXI.r.ready    := false.B
        io.AXI.aw.valid   := false.B
        io.AXI.w.valid    := false.B
        io.AXI.b.ready    := false.B
        io.IDU_2_EXU.ready           <> false.B
        io.out.valid          <> false.B
    }

    io.AXI.ar.bits.addr  <> io.IDU_2_EXU.bits.EXU_A + io.IDU_2_EXU.bits.Imm
    io.AXI.aw.bits.addr  <> io.IDU_2_EXU.bits.EXU_A + io.IDU_2_EXU.bits.Imm
    io.AXI.w.bits.data   <> (io.IDU_2_EXU.bits.EXU_B << (io.AXI.aw.bits.addr(1,0) << 3.U))(31, 0)
    
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
        LSU_PC.io.valid := io.out.valid && io.out.ready && !reset.asBool
    }
}
