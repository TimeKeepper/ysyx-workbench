import homework._

object Elaborate extends App {
  val firtoolOptions = Array(
    "--lowering-options=" + List(
      // make vivado happy
      // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
      "mitigateVivadoArrayIndexConstPropBug",
      "locationInfoStyle=wrapInAtSquareBracket"
    ).reduce(_ + "," + _)
  )
  circt.stage.ChiselStage.emitSystemVerilogFile(new BASYS.II_4(), args, firtoolOptions)
}
