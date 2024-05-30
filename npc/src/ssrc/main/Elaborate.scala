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
  println("Chisel version: " + ChiselVersion.current)
  circt.stage.ChiselStage.emitSystemVerilogFile(new riscv_cpu.ALU(), args, firtoolOptions)
}
