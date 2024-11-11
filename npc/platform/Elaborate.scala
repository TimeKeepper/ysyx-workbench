package config

import chisel3._

object Elaborateysyxsoc extends App {
  val firtoolOptions = Array(
    "-disable-all-randomization",
    "-strip-debug-info",
    "--lowering-options=" + List(
      // make yosys happy
      // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
      "disallowLocalVariables",
      "disallowPackedArrays",
      "locationInfoStyle=wrapInAtSquareBracket"
    ).reduce(_ + "," + _)
  )
  
  Config.Reset_Vector = "h30000000".U(32.W)
  Config.setDPIC(true)
  Config.setIcacheParam(2, 4, 19, "ha0000000")

  circt.stage.ChiselStage.emitSystemVerilogFile(gen = new riscv_cpu.ysyx_23060198(), args = args, firtoolOpts  = firtoolOptions)
}

object Elaboratenpc extends App {
  val firtoolOptions = Array(
    "-disable-all-randomization",
    "-strip-debug-info",
    "--lowering-options=" + List(
      // make yosys happy
      // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
      "disallowLocalVariables",
      "disallowPackedArrays",
      "locationInfoStyle=wrapInAtSquareBracket"
    ).reduce(_ + "," + _)
  )
  
  Config.Reset_Vector = "h80000000".U(32.W)
  Config.setDPIC(true)
  Config.setIcacheParam(2, 4, 19, "h80000000")

  circt.stage.ChiselStage.emitSystemVerilogFile(new riscv_cpu.top(), args, firtoolOptions)
}

object Elaboratecore extends App {
  val firtoolOptions = Array(
    "--lowering-options=" + List(
      // make yosys happy
      // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
      "disallowLocalVariables",
      "disallowPackedArrays",
      "locationInfoStyle=wrapInAtSquareBracket"
    ).reduce(_ + "," + _)
  )
  
  Config.Reset_Vector = "h80000000".U(32.W)
  Config.setDPIC(false)
  Config.setIcacheParam(2, 4, 19, "h80000000")

  circt.stage.ChiselStage.emitSystemVerilogFile(new riscv_cpu.ysyx_23060198(), args, firtoolOptions)
}
