import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

object Elaborate extends App {
  val firtoolOptions = Array(
    "--lowering-options=" + List(
      // make yosys happy
      // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
      "disallowLocalVariables",
      "disallowPackedArrays",
      "locationInfoStyle=wrapInAtSquareBracket"
    ).reduce(_ + "," + _)
  )
  circit.stage.ChiselStage.emitSystemVerilogFile(new npc.npc(), args, firtoolOptions)
}
