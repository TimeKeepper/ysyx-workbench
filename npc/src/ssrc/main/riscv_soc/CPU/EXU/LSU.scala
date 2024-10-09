package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._
import config._

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
    |  import "DPI-C" function void IFU_finished();
    |  always @(posedge clock) begin
    |    if(valid) begin
    |      IFU_finished();
    |    end
    |  end
    |endmodule
    """.stripMargin)
}

class ysyx_23060198_LSU extends Module{
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(new Bundle{
            val GNU_io    = Input(new GNU_Output)
        }))

        val out = Decoupled(new Bundle{
            val Mem_rdata  = Output(UInt(32.W))
        })
        val AXI = new AXI_Master
    })
    
    val s_idle :: s_wait_addr :: s_wait_data :: Nil = Enum(3)

    val state_write = RegInit(s_idle)

    when(io.in.bits.GNU_io.MemWr) {
        io.AXI.araddr.valid   := false.B
        io.AXI.rdata.ready    := false.B
        io.AXI.awaddr.valid   <> io.in.valid
        io.AXI.wdata.valid    := io.in.valid
        io.AXI.bresp.ready    <> io.out.ready
        io.AXI.bresp.valid    <> io.out.valid

        when(state_write === s_idle) {
            io.in.ready := io.AXI.awaddr.ready && io.AXI.wdata.ready

            when(io.in.valid){
                when(io.AXI.awaddr.ready && io.AXI.wdata.ready){
                    state_write := s_idle
                }.elsewhen(io.AXI.awaddr.ready && !io.AXI.wdata.ready){
                    state_write := s_wait_data
                }.elsewhen(!io.AXI.awaddr.ready && io.AXI.wdata.ready){
                    state_write := s_wait_addr
                }.otherwise{
                    state_write := s_idle
                }
            }
        }.elsewhen(state_write === s_wait_addr){
            io.in.ready := io.AXI.awaddr.ready
            when(io.in.valid && io.AXI.awaddr.ready){
                state_write := s_idle
            }
        }.elsewhen(state_write === s_wait_data){
            io.in.ready := io.AXI.wdata.ready
            when(io.in.valid && io.AXI.wdata.ready){
                state_write := s_idle
            }
        }.otherwise{
            io.in.ready := false.B
        }
    }.elsewhen(io.in.bits.GNU_io.MemtoReg) {
        io.AXI.awaddr.valid   := false.B
        io.AXI.wdata.valid    := false.B
        io.AXI.bresp.ready    := false.B
        io.AXI.araddr.ready   <> io.in.ready
        io.AXI.araddr.valid   <> io.in.valid
        io.AXI.rdata.valid    <> io.out.valid
        io.AXI.rdata.ready    <> io.out.ready
    }.otherwise {
        io.AXI.araddr.valid   := false.B
        io.AXI.rdata.ready    := false.B
        io.AXI.awaddr.valid   := false.B
        io.AXI.wdata.valid    := false.B
        io.AXI.bresp.ready    := false.B
        io.in.ready           <> false.B
        io.out.valid          <> false.B
    }

    io.AXI.araddr.bits.addr  <> io.in.bits.GNU_io.GPR_Adata + io.in.bits.GNU_io.Imm
    io.AXI.awaddr.bits.addr  <> io.in.bits.GNU_io.GPR_Adata + io.in.bits.GNU_io.Imm
    io.AXI.wdata.bits.data   <> (io.in.bits.GNU_io.GPR_Bdata << (io.AXI.awaddr.bits.addr(1,0) << 3.U))(31, 0)
    
    when(io.in.bits.GNU_io.MemOp === MemOp_1BU || io.in.bits.GNU_io.MemOp === MemOp_1BS){
        io.AXI.wdata.bits.strb   := MuxLookup(io.AXI.awaddr.bits.addr(1,0), "b0001".U)(Seq(
            "b00".U -> "b0001".U,
            "b01".U -> "b0010".U,
            "b10".U -> "b0100".U,
            "b11".U -> "b1000".U,
        ))
    }.elsewhen(io.in.bits.GNU_io.MemOp === MemOp_2BU || io.in.bits.GNU_io.MemOp === MemOp_2BS){
        io.AXI.wdata.bits.strb   := MuxLookup(io.AXI.awaddr.bits.addr(1,0), "b0011".U)(Seq(
            "b00".U -> "b0011".U,
            "b01".U -> "b0110".U,
            "b10".U -> "b1100".U,
        ))
    }.otherwise{
        io.AXI.wdata.bits.strb   := "b1111".U
    }
    // io.AXI.wdata.bits.strb   := MuxLookup(io.in.bits.GNU_io.MemOp, "b1111".U)(Seq(
    //     MemOp_1BU -> "b0001".U,
    //     MemOp_1BS -> "b0001".U,
    //     MemOp_2BU -> "b0011".U,
    //     MemOp_2BS -> "b0011".U,
    //     MemOp_4BU -> "b1111".U,
    // ))
    io.AXI.awaddr.bits.size  := MuxLookup(io.in.bits.GNU_io.MemOp, 0.U)(Seq(
        MemOp_1BU -> 0.U,
        MemOp_1BS -> 0.U,
        MemOp_2BU -> 1.U,
        MemOp_2BS -> 1.U,
        MemOp_4BU -> 2.U,
    ))
    io.AXI.araddr.bits.size  := MuxLookup(io.in.bits.GNU_io.MemOp, 0.U)(Seq(
        MemOp_1BU -> 0.U,
        MemOp_1BS -> 0.U,
        MemOp_2BU -> 1.U,
        MemOp_2BS -> 1.U,
        MemOp_4BU -> 2.U,
    ))

    val u_mem_rd = Wire(UInt(32.W))
    val s_mem_rd = Wire(SInt(32.W))

    u_mem_rd := MuxLookup(io.in.bits.GNU_io.MemOp, 0.U)(Seq(
        MemOp_1BU -> (io.AXI.rdata.bits.data(7,0).asUInt),
        MemOp_1BS -> (io.AXI.rdata.bits.data(7,0).asUInt),
        MemOp_2BU -> (io.AXI.rdata.bits.data(15,0).asUInt),
        MemOp_2BS -> (io.AXI.rdata.bits.data(15,0).asUInt),
        MemOp_4BU -> (io.AXI.rdata.bits.data(31,0).asUInt),
    ))
    
    s_mem_rd := MuxLookup(io.in.bits.GNU_io.MemOp, 0.S)(Seq(
        MemOp_1BU -> (io.AXI.rdata.bits.data(7,0)).asSInt,
        MemOp_1BS -> (io.AXI.rdata.bits.data(7,0)).asSInt,
        MemOp_2BU -> (io.AXI.rdata.bits.data(15,0)).asSInt,
        MemOp_2BS -> (io.AXI.rdata.bits.data(15,0)).asSInt,
        MemOp_4BU -> (io.AXI.rdata.bits.data(31,0)).asSInt,
    ))

    when(io.in.bits.GNU_io.MemOp === MemOp_1BU || io.in.bits.GNU_io.MemOp === MemOp_2BU || io.in.bits.GNU_io.MemOp === MemOp_4BU){
        io.out.bits.Mem_rdata := u_mem_rd
    }.otherwise{
        io.out.bits.Mem_rdata := s_mem_rd.asUInt
    }

    if(Config.DPIC_on){
        val LS_DPIC = Module(new LSU_DPIC)
        LS_DPIC.io.LS_begin  := io.AXI.araddr.valid || io.AXI.awaddr.valid
        LS_DPIC.io.addr      := io.in.bits.GNU_io.GPR_Adata + io.in.bits.GNU_io.Imm

        val LSU_PC = Module(new LSU_PC)
        LSU_PC.io.clock := clock
        LSU_PC.io.valid := io.out.valid && io.out.ready && !reset.asBool
    }
}
