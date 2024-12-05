package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._
import config._

class WBU_catch extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val valid = Input(Bool())
        
        val next_pc = Input(UInt(32.W))

        val gpr_waddr = Input(UInt(32.W))
        val gpr_wdata = Input(UInt(32.W))

        val csr_wen = Input(UInt(32.W))
        val csr_waddr = Input(UInt(32.W))
        val csr_wdata = Input(UInt(32.W))
    })

    setInline("WBU_catch.v",
    """module WBU_catch(
    |    input clock,
    |    input valid,
    |
    |    input [31:0] next_pc,
    |
    |    input [31:0] gpr_waddr,
    |    input [31:0] gpr_wdata,
    |
    |    input [31:0] csr_wen,
    |    input [31:0] csr_waddr,
    |    input [31:0] csr_wdata
    |);
    |
    |   import "DPI-C" function void WBU_catch(input int unsigned next_pc, input int unsigned gpr_waddr, input int unsigned gpr_wdata, input int unsigned csr_wen, input int unsigned csr_waddr, input int unsigned csr_wdata);
    |   always @(posedge clock) begin
    |       if(valid) begin
    |           WBU_catch(next_pc, gpr_waddr, gpr_wdata, csr_wen, csr_waddr, csr_wdata);
    |       end
    |   end
    |
    |endmodule
    """.stripMargin)
}

class WBU extends Module {
    val io = IO(new Bundle{
        val EXU_2_WBU = Flipped(Decoupled(Input(new BUS_EXU_2_WBU)))
        val WBU_2_IFU = Decoupled(Output(new BUS_WBU_2_IFU))
        val WBU_2_REG = Output(new BUS_WBU_2_REG)
    })

    val state = RegInit(bus_state.s_wait_ready)

    state := MuxLookup(state, bus_state.s_wait_valid)(
        Seq(
            bus_state.s_wait_valid -> Mux(io.EXU_2_WBU.valid, bus_state.s_wait_ready, bus_state.s_wait_valid),
            bus_state.s_wait_ready -> Mux(io.WBU_2_IFU.ready, bus_state.s_wait_valid, bus_state.s_wait_ready),
        )
    )

    io.WBU_2_IFU.valid := state === bus_state.s_wait_ready && !reset.asBool // 这是由于soc外设的行为不确定而做出的改动
    io.EXU_2_WBU.ready  := state === bus_state.s_wait_valid
    
    when(io.EXU_2_WBU.valid && io.EXU_2_WBU.ready){
        io.WBU_2_REG.inst_valid := true.B
    }.otherwise{
        io.WBU_2_REG.inst_valid := false.B
    }
    
    val Default_Next_Pc = io.EXU_2_WBU.bits.PC + 4.U

    val Next_Pc = MuxLookup(io.EXU_2_WBU.bits.Branch, io.EXU_2_WBU.bits.Jmp_Pc)(Seq(
        Bran_TypeEnum.Bran_Jeq -> Mux(io.EXU_2_WBU.bits.Result === 0.U, io.EXU_2_WBU.bits.Jmp_Pc, Default_Next_Pc),
        Bran_TypeEnum.Bran_Jne -> Mux(io.EXU_2_WBU.bits.Result === 0.U, Default_Next_Pc, io.EXU_2_WBU.bits.Jmp_Pc),
        Bran_TypeEnum.Bran_Jlt -> Mux(io.EXU_2_WBU.bits.Result(0), io.EXU_2_WBU.bits.Jmp_Pc, Default_Next_Pc),
        Bran_TypeEnum.Bran_Jge -> Mux(io.EXU_2_WBU.bits.Result(0), Default_Next_Pc, io.EXU_2_WBU.bits.Jmp_Pc),
        Bran_TypeEnum.Bran_NJmp -> Default_Next_Pc,
    ))

    val GPR_wdata = MuxLookup(io.EXU_2_WBU.bits.MemtoReg, io.EXU_2_WBU.bits.Result)(Seq(
        Y  -> io.EXU_2_WBU.bits.Mem_rdata,
        N  -> Mux(io.EXU_2_WBU.bits.csr_ctr === CSR_TypeEnum.CSR_N, Mux(io.EXU_2_WBU.bits.Branch === Bran_TypeEnum.Bran_Jmpr || io.EXU_2_WBU.bits.Branch === Bran_TypeEnum.Bran_Jmp, Default_Next_Pc, io.EXU_2_WBU.bits.Result), io.EXU_2_WBU.bits.CSR_rdata),
    ))

    val CSR_waddra = MuxLookup(io.EXU_2_WBU.bits.csr_ctr, io.EXU_2_WBU.bits.CSR_waddr)(Seq(
        CSR_TypeEnum.CSR_R1W2 -> "h341".U
    ))

    val CSR_wdataa = MuxLookup(io.EXU_2_WBU.bits.csr_ctr, io.EXU_2_WBU.bits.Result)(Seq(
        CSR_TypeEnum.CSR_R1W2 -> io.EXU_2_WBU.bits.PC,
    ))

    io.WBU_2_REG.Next_Pc       := Next_Pc
    io.WBU_2_REG.GPR_waddr     := io.EXU_2_WBU.bits.GPR_waddr
    io.WBU_2_REG.GPR_wdata     := GPR_wdata
    io.WBU_2_REG.CSR_ctr       <> io.EXU_2_WBU.bits.csr_ctr
    io.WBU_2_REG.CSR_waddra    := CSR_waddra
    io.WBU_2_REG.CSR_waddrb    := "h342".U
    io.WBU_2_REG.CSR_wdataa    := CSR_wdataa
    io.WBU_2_REG.CSR_wdatab    := 11.U

    if(Config.Simulate){
        val state_idle :: state_catch :: Nil = Enum(2)

        val state_Catch = RegInit(state_idle)
        when(io.WBU_2_IFU.fire && !reset.asBool) {
            state_Catch := state_catch
        }

        val Catch = Module(new WBU_catch)
        Catch.io.clock := clock
        Catch.io.valid := io.EXU_2_WBU.fire && !reset.asBool && (state_Catch === state_catch)

        Catch.io.next_pc := Next_Pc
        
        Catch.io.gpr_waddr := io.EXU_2_WBU.bits.GPR_waddr
        Catch.io.gpr_wdata := GPR_wdata

        Catch.io.csr_wen := io.EXU_2_WBU.bits.csr_ctr =/= CSR_TypeEnum.CSR_N
        Catch.io.csr_waddr := CSR_waddra
        Catch.io.csr_wdata := CSR_wdataa
    }

}