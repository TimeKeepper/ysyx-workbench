package decoder

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

class BCDDecoder extends Module {
    val io = IO(new Bundle{
        val in = Input(UInt(4.W))
        val out = Output(UInt(8.W))
    })

    val num_0 = "b11111101".U
    val num_1 = "b01100000".U
    val num_2 = "b11011010".U
    val num_3 = "b11110010".U
    val num_4 = "b01100110".U
    val num_5 = "b10110110".U
    val num_6 = "b10111110".U
    val num_7 = "b11100000".U
    val num_8 = "b11111110".U
    val num_9 = "b11110110".U

    val out = MuxLookup(io.in, 0.U)(Seq(
        "h0".U -> num_0,
        "h1".U -> num_1,
        "h2".U -> num_2,
        "h3".U -> num_3,
        "h4".U -> num_4,
        "h5".U -> num_5,
        "h6".U -> num_6,
        "h7".U -> num_7,
        "h8".U -> num_8,
        "h9".U -> num_9
    ))

}