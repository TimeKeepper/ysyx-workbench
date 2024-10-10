package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._
import Instructions._
// riscv generating number(all meassge ALU and other thing needs) unit

class ysyx_23060198_GNU extends Module{
    val io = IO(new Bundle{
        val IFU_2_GNU     = Flipped(Decoupled(Input(new BUS_IFU_2_GNU)))
        val REG_2_GNU     = Input(new BUS_REG_2_GNU)
        
        val GNU_2_EXU     = Decoupled(Output(new BUS_GNU_2_EXU))
        val GNU_2_REG     = Output(new BUS_GNU_2_REG)
    })

    val state = RegInit(s_wait_valid)

    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.IFU_2_GNU.valid, s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.GNU_2_EXU.ready, s_wait_valid, s_wait_ready),
        )
    )

    val idu = Module(new ysyx_23060198_IDU)
    val igu = Module(new ysyx_23060198_IGU)

    io.GNU_2_EXU.valid := state === s_wait_ready
    io.IFU_2_GNU.ready := state === s_wait_valid
    val comunication_succeed = (io.IFU_2_GNU.valid && io.IFU_2_GNU.ready)

    igu.io.inst     <> io.IFU_2_GNU.bits.data
    igu.io.ExtOp    <> idu.io.ExtOp

    idu.io.inst     <> io.IFU_2_GNU.bits.data

    io.GNU_2_EXU.bits.RegWr        <> RegEnable(idu.io.RegWr,          comunication_succeed) 
    io.GNU_2_EXU.bits.Branch       <> RegEnable(idu.io.Branch,         comunication_succeed) 
    io.GNU_2_EXU.bits.MemtoReg     <> RegEnable(idu.io.MemtoReg,       comunication_succeed) 
    io.GNU_2_EXU.bits.MemWr        <> RegEnable(idu.io.MemWr,          comunication_succeed) 
    io.GNU_2_EXU.bits.MemOp        <> RegEnable(idu.io.MemOp,          comunication_succeed) 
    io.GNU_2_EXU.bits.ALUAsrc      <> RegEnable(idu.io.ALUAsrc,        comunication_succeed) 
    io.GNU_2_EXU.bits.ALUBsrc      <> RegEnable(idu.io.ALUBsrc,        comunication_succeed) 
    io.GNU_2_EXU.bits.ALUctr       <> RegEnable(idu.io.ALUctr,         comunication_succeed) 
    io.GNU_2_EXU.bits.csr_ctr      <> RegEnable(idu.io.csr_ctr,        comunication_succeed) 
    io.GNU_2_EXU.bits.Imm          <> RegEnable(igu.io.imm,            comunication_succeed) 
    io.GNU_2_EXU.bits.GPR_Adata    <> RegEnable(io.REG_2_GNU.GPR_Adata,  comunication_succeed) 
    io.GNU_2_EXU.bits.GPR_Bdata    <> RegEnable(io.REG_2_GNU.GPR_Bdata,  comunication_succeed) 
    io.GNU_2_EXU.bits.GPR_waddr    <> RegEnable(io.IFU_2_GNU.bits.data(11, 7), comunication_succeed) 
    io.GNU_2_EXU.bits.PC           <> RegEnable(io.REG_2_GNU.PC,         comunication_succeed) 
    io.GNU_2_REG.CSR_raddr           <> RegEnable(MuxLookup(
                                                        idu.io.csr_ctr, igu.io.imm(11, 0))(
                                                            Seq(
                                                                CSR_R1W0 -> "h341".U,
                                                                CSR_R1W2 -> "h305".U,
                                                            )
                                                    ), comunication_succeed
                                        )
}