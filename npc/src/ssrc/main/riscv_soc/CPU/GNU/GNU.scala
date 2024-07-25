package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._
import Instructions._
// riscv generating number(all meassge ALU and other thing needs) unit

class GNU extends Module{
    val io = IO(new Bundle{
        // Form IFU
        val in          = Flipped(Decoupled(new Bundle{
            val IFU_io     = new IFU_Output
            val PC         = UInt(32.W)
            val GPR_Adata  = UInt(32.W)
            val GPR_Bdata  = UInt(32.W)
        }))

        val out         = Decoupled(new Bundle{
            val GNU_io     = new GNU_Output

            // To Register File
            val CSR_raddr   = Output(UInt(12.W))
        })
    })

    val state = RegInit(s_wait_valid)

    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.in.valid,  s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.out.ready, s_wait_valid, s_wait_ready),
        )
    )

    val idu = Module(new IDU)
    val igu = Module(new IGU)

    io.out.valid := state === s_wait_ready
    io.in.ready  := state === s_wait_valid
    val comunication_succeed = (io.in.valid && io.in.ready)

    igu.io.inst     <> io.in.bits.IFU_io.data
    igu.io.ExtOp    <> idu.io.ExtOp

    idu.io.inst     <> io.in.bits.IFU_io.data

    io.out.bits.GNU_io.RegWr        <> RegEnable(idu.io.RegWr,          comunication_succeed) 
    io.out.bits.GNU_io.Branch       <> RegEnable(idu.io.Branch,         comunication_succeed) 
    io.out.bits.GNU_io.MemtoReg     <> RegEnable(idu.io.MemtoReg,       comunication_succeed) 
    io.out.bits.GNU_io.MemWr        <> RegEnable(idu.io.MemWr,          comunication_succeed) 
    io.out.bits.GNU_io.MemOp        <> RegEnable(idu.io.MemOp,          comunication_succeed) 
    io.out.bits.GNU_io.ALUAsrc      <> RegEnable(idu.io.ALUAsrc,        comunication_succeed) 
    io.out.bits.GNU_io.ALUBsrc      <> RegEnable(idu.io.ALUBsrc,        comunication_succeed) 
    io.out.bits.GNU_io.ALUctr       <> RegEnable(idu.io.ALUctr,         comunication_succeed) 
    io.out.bits.GNU_io.csr_ctr      <> RegEnable(idu.io.csr_ctr,        comunication_succeed) 
    io.out.bits.GNU_io.Imm          <> RegEnable(igu.io.imm,            comunication_succeed) 
    io.out.bits.GNU_io.GPR_Adata    <> RegEnable(io.in.bits.GPR_Adata,  comunication_succeed) 
    io.out.bits.GNU_io.GPR_Bdata    <> RegEnable(io.in.bits.GPR_Bdata,  comunication_succeed) 
    io.out.bits.GNU_io.GPR_waddr    <> RegEnable(io.in.bits.IFU_io.data(11, 7), comunication_succeed) 
    io.out.bits.GNU_io.PC           <> RegEnable(io.in.bits.PC,         comunication_succeed) 
    io.out.bits.CSR_raddr           <> RegEnable(MuxLookup(
                                                        idu.io.csr_ctr, igu.io.imm(11, 0))(
                                                            Seq(
                                                                CSR_R1W0 -> "h341".U,
                                                                CSR_R1W2 -> "h305".U,
                                                            )
                                                    ), comunication_succeed
                                        )
}