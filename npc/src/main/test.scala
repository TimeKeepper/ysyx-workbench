class test extends Module {
  val io = IO(new Bundle {
    val input   = Input(UInt(16.W))
    val output  = Output(UInt(16.W))
  })

  io.output := io.input + 1.U
}
