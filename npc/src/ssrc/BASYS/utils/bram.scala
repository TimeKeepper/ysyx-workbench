package BASYS

import chisel3._
import chisel3.util._

class BRAM_sim(width_addr: Int, width_data: Int, name: String) extends BlackBox with HasBlackBoxInline{
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val en    = Input(Bool())
        val addr  = Input(UInt(width_addr.W))
        val dout  = Output(UInt(width_data.W))
    })

    val func = s"bram_${name}_read"

    val code =
        s"""
        |module BRAM_sim(
        |    input wire clock,
        |    input wire en,
        |    input wire [${width_addr-1}:0] addr,
        |    output wire [${width_data-1}:0] dout
        |);
        |    import "DPI-C" function void ${func}(input int raddr, output int rdata);
        |    always @(posedge clock) begin
        |        if (en) ${func}({${32 - width_addr}'b0, addr}, {${32 - width_data}'b0, dout});
        |    end
        |endmodule
        """

    setInline("BRAM_sim.v", code.stripMargin)
}

class BRAM(width_addr: Int, width_data: Int) extends BlackBox{
    val io = IO(new Bundle{
        val clka = Input(Clock())
        val ena = Input(Bool())
        val addra = Input(UInt(width_addr.W))
        val douta = Output(UInt(width_data.W))
    })
}

class BRAM_0(width_addr: Int, width_data: Int) extends BlackBox{
    val io = IO(new Bundle{
        val clka = Input(Clock())
        val ena = Input(Bool())
        val addra = Input(UInt(width_addr.W))
        val douta = Output(UInt(width_data.W))
    })
}

class BRAM_P extends BlackBox{
    val io = IO(new Bundle{
        val clka = Input(Clock())
        val ena = Input(Bool())
        val addra = Input(UInt(9.W))
        val douta = Output(UInt(12.W))
    })
}
