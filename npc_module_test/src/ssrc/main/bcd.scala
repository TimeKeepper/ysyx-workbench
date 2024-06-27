package decoder

import chisel3._
import chisel3.util._
import chisel3.util.MuxLookup

class BCDDecoder extends Module {
    val io = IO(new Bundle{
        val in = Input(UInt(4.W))
        val out = Output(UInt(8.W))
    })

    val num_0 = "b00111111".U
    val num_1 = "b00000110".U
    val num_2 = "b01011011".U
    val num_3 = "b01001111".U
    val num_4 = "b01100110".U
    val num_5 = "b01101101".U
    val num_6 = "b01111101".U
    val num_7 = "b00000111".U
    val num_8 = "b01111111".U
    val num_9 = "b01101111".U
    val num_A = "b01110111".U
    val num_B = "b01111111".U
    val num_C = "b10011101".U
    val num_D = "b01111010".U
    val num_E = "b10011110".U
    val num_F = "b10001110".U

    io.out := MuxLookup(io.in, 0.U)(Seq(
        "h0".U -> num_0,
        "h1".U -> num_1,
        "h2".U -> num_2,
        "h3".U -> num_3,
        "h4".U -> num_4,
        "h5".U -> num_5,
        "h6".U -> num_6,
        "h7".U -> num_7,
        "h8".U -> num_8,
        "h9".U -> num_9,
        "hA".U -> num_A,
        "hB".U -> num_B,
        "hC".U -> num_C,
        "hD".U -> num_D,
        "hE".U -> num_E,
        "hF".U -> num_F
    ))

}