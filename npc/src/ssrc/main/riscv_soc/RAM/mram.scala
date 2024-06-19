package ram

import chisel3._

class MRAM_in extends Bundle {
    val addr = Input(UInt(32.W))
} 

class MRAM_out extends Bundle {
    val data = Output(UInt(32.W))
}

class MyRAM extends Module {
    val io = IO(new Bundle {
        val in  = new MRAM_in
        val out = new MRAM_out
    })

    io.out.data := MuxLookup(io.in.addr, 0.U(32.W))(Seq(
        0x80000000.U(32.W) -> 0x00000513.U(32.W), // li a0 0
        0x80000004.U(32.W) -> 0x00150513.U(32.W), // add a0 a0 1
        0x80000008.U(32.W) -> 0x00a12023.U(32.W), // sw a0 0(sp)
        0x8000000c.U(32.W) -> 0xffbfffef.U(32.W)  // jmp 0x80000004
    ))
}