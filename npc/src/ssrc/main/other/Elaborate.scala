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
  // circt.stage.ChiselStage.emitSystemVerilogFile(new homework.PS2Receiver(), args, firtoolOptions)
  println(getVerilogString(new homework.PS2Receiver()))
}
