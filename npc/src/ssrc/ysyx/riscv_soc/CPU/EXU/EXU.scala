package riscv_cpu

import chisel3._
import chisel3.util._

import signal_value._
import bus_state._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

// riscv excution unit

// class EXU(idBits: Int)(implicit p: Parameters) extends LazyModule {
//     val masterNode = AXI4MasterNode(p(ExtIn).map(params =>
//         AXI4MasterPortParameters(
//         masters = Seq(AXI4MasterParameters(
//             name = "exu",
//             id   = IdRange(0, 1 << idBits))))).toSeq)
//     lazy val module = new Impl
//     class Impl extends LazyModuleImp(this) {
//         val io = IO(new Bundle{
//             // From CSR
//             val IDU_2_EXU = Flipped(Decoupled(Input(new BUS_IDU_2_EXU)))
//             val REG_2_EXU = Input(new BUS_REG_2_EXU)

//             val EXU_2_WBU = Decoupled(Output(new BUS_EXU_2_WBU))

//             val is_ls_dispatch = Output(Bool()) // ugly, should remove in the future
//         })
//         val is_ls_dispatch = RegInit(false.B)
//         io.is_ls_dispatch := is_ls_dispatch

//         val AXI = Wire(AXI4Bundle(CPUAXI4BundleParameters()))
//         val (master, _) = masterNode.out(0)
//         master <> AXI

//         val alu = Module(new ALU)
//         val lsu = Module(new LSU)

//         when(io.IDU_2_EXU.fire) {
//             is_ls_dispatch := io.IDU_2_EXU.bits.EXUctr === EXUctr_TypeEnum.EXUctr_LD ||
//                               io.IDU_2_EXU.bits.EXUctr === EXUctr_TypeEnum.EXUctr_ST
//         }.elsewhen(io.EXU_2_WBU.fire){
//             is_ls_dispatch := false.B
//         }

//         when(is_ls_dispatch){
//             alu.io.IDU_2_EXU.valid := false.B
//             alu.io.out.ready := false.B
//             io.IDU_2_EXU.ready <> lsu.io.IDU_2_EXU.ready
//             io.IDU_2_EXU.valid <> lsu.io.IDU_2_EXU.valid
//             io.EXU_2_WBU.ready <> lsu.io.out.ready
//             io.EXU_2_WBU.valid <> lsu.io.out.valid
//         }.otherwise{
//             lsu.io.IDU_2_EXU.valid := false.B
//             lsu.io.out.ready := false.B
//             io.IDU_2_EXU.ready <> alu.io.IDU_2_EXU.ready
//             io.IDU_2_EXU.valid <> alu.io.IDU_2_EXU.valid
//             io.EXU_2_WBU.ready <> alu.io.out.ready
//             io.EXU_2_WBU.valid <> alu.io.out.valid
//         }

//         alu.io.IDU_2_EXU.bits := io.IDU_2_EXU.bits

//         lsu.io.IDU_2_EXU.bits := io.IDU_2_EXU.bits
//         lsu.io.AXI <> AXI
        
//         val Jmp_Pc = MuxLookup(io.IDU_2_EXU.bits.Branch, io.IDU_2_EXU.bits.PC + io.IDU_2_EXU.bits.Imm)(Seq(
//             Bran_TypeEnum.Bran_Jmpr -> (io.IDU_2_EXU.bits.EXU_A + io.IDU_2_EXU.bits.Imm),
//             Bran_TypeEnum.Bran_Jcsr -> (io.IDU_2_EXU.bits.EXU_B)
//         ))

//         io.EXU_2_WBU.bits.Branch        := io.IDU_2_EXU.bits.Branch      
//         io.EXU_2_WBU.bits.Jmp_Pc        := Jmp_Pc                        
//         io.EXU_2_WBU.bits.MemtoReg      := io.IDU_2_EXU.bits.EXUctr  === EXUctr_TypeEnum.EXUctr_LD 
//         io.EXU_2_WBU.bits.csr_ctr       := io.IDU_2_EXU.bits.csr_ctr     
//         io.EXU_2_WBU.bits.CSR_waddr     := io.IDU_2_EXU.bits.Imm(11, 0)  
//         io.EXU_2_WBU.bits.GPR_waddr     := io.IDU_2_EXU.bits.GPR_waddr   
//         io.EXU_2_WBU.bits.PC            := io.IDU_2_EXU.bits.PC          
//         io.EXU_2_WBU.bits.CSR_rdata     := io.IDU_2_EXU.bits.EXU_B   
//         io.EXU_2_WBU.bits.Result        := alu.io.out.bits.Result
//         io.EXU_2_WBU.bits.Mem_rdata     := lsu.io.out.bits.Mem_rdata
//     }
// }