package riscv_cpu

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import org.chipsalliance.rvdecoderdb

import signal_value._
import bus_state._
// riscv generating number(all meassge ALU and other thing needs) unit

case class rvInstructionPattern(val inst: rvdecoderdb.instruction) extends DecodePattern {
    override def bitPat: BitPat = BitPat("b" + inst.encoding.toString())
}

class ysyx_23060198_IDU extends Module{
    val io = IO(new Bundle{
        val IFU_2_IDU     = Flipped(Decoupled(Input(new BUS_IFU_2_IDU)))
        val REG_2_IDU     = Input(new BUS_REG_2_IDU)

        val IDU_2_EXU     = Decoupled(Output(new BUS_IDU_2_EXU))
        val IDU_2_REG     = Output(new BUS_IDU_2_REG)
    })

    val state = RegInit(bus_state.s_wait_valid)

    state := MuxLookup(state, bus_state.s_wait_valid)(
        Seq(
            bus_state.s_wait_valid -> Mux(io.IFU_2_IDU.valid, bus_state.s_wait_ready, bus_state.s_wait_valid),
            bus_state.s_wait_ready -> Mux(io.IDU_2_EXU.ready, bus_state.s_wait_valid, bus_state.s_wait_ready),
        )
    )

    io.IDU_2_EXU.valid := state === bus_state.s_wait_ready
    io.IFU_2_IDU.ready := state === bus_state.s_wait_valid
    val comunication_succeed = (io.IFU_2_IDU.valid && io.IFU_2_IDU.ready)

    val decodeTable = new DecodeTable(my_fooldecodedb.possiblePattern, my_fooldecodedb.allFields)
    val decodeResult = decodeTable.decode(io.IFU_2_IDU.bits.data)

    val instTable = rvdecoderdb.fromFile.instructions(os.pwd / "rvdecoderdb" / "rvdecoderdbtest" / "jvm" / "riscv-opcodes")

    val imm = MuxLookup(decodeResult(ImmField), 0.U)(
        Seq(
            Imm_TypeEnum.Imm_I -> Cat(Fill(21, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(31, 20)),
            Imm_TypeEnum.Imm_U -> Cat(io.IFU_2_IDU.bits.data(31, 12), Fill(12, 0.U)),
            Imm_TypeEnum.Imm_S -> Cat(Fill(20, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(31, 25), io.IFU_2_IDU.bits.data(11, 7)),
            Imm_TypeEnum.Imm_B -> Cat(Fill(20, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(7), io.IFU_2_IDU.bits.data(30, 25), io.IFU_2_IDU.bits.data(11, 8), 0.U(1.W)),
            Imm_TypeEnum.Imm_J -> Cat(Fill(12, io.IFU_2_IDU.bits.data(31)), io.IFU_2_IDU.bits.data(19, 12), io.IFU_2_IDU.bits.data(20), io.IFU_2_IDU.bits.data(30, 21), 0.U(1.W)),
        )
    )

    val csr_raddr = MuxLookup(decodeResult(csr_ctrField), imm(11, 0))(
        Seq(
            CSR_TypeEnum.CSR_R1W0 -> "h341".U,
            CSR_TypeEnum.CSR_R1W2 -> "h305".U,
        )
    )

    val gpr_waddr = Mux(decodeResult(RegWrFiled), io.IFU_2_IDU.bits.data(10, 7), 0.U(4.W))

    io.IDU_2_REG.CSR_raddr         <> csr_raddr

    io.IDU_2_EXU.bits.Branch       <> RegEnable(decodeResult(BranchField),      comunication_succeed) 
    io.IDU_2_EXU.bits.MemtoReg     <> RegEnable(decodeResult(MemtoRegField),    comunication_succeed) 
    io.IDU_2_EXU.bits.MemWr        <> RegEnable(decodeResult(MemWrField),       comunication_succeed) 
    io.IDU_2_EXU.bits.MemOp        <> RegEnable(decodeResult(MemOpField),       comunication_succeed) 
    io.IDU_2_EXU.bits.ALUAsrc      <> RegEnable(decodeResult(ALUAsrcField),     comunication_succeed) 
    io.IDU_2_EXU.bits.ALUBsrc      <> RegEnable(decodeResult(ALUBsrcField),     comunication_succeed) 
    io.IDU_2_EXU.bits.ALUctr       <> RegEnable(decodeResult(ALUctrField),      comunication_succeed) 
    io.IDU_2_EXU.bits.csr_ctr      <> RegEnable(decodeResult(csr_ctrField),     comunication_succeed) 
    io.IDU_2_EXU.bits.Imm          <> RegEnable(imm,                    comunication_succeed) 
    io.IDU_2_EXU.bits.GPR_Adata    <> RegEnable(io.REG_2_IDU.GPR_Adata,  comunication_succeed) 
    io.IDU_2_EXU.bits.GPR_Bdata    <> RegEnable(io.REG_2_IDU.GPR_Bdata,  comunication_succeed) 
    io.IDU_2_EXU.bits.GPR_waddr    <> RegEnable(gpr_waddr, comunication_succeed) 
    io.IDU_2_EXU.bits.PC           <> RegEnable(io.REG_2_IDU.PC,         comunication_succeed) 
    io.IDU_2_EXU.bits.CSR_rdata    <> RegEnable(io.REG_2_IDU.CSR_rdata,  comunication_succeed) 
}
