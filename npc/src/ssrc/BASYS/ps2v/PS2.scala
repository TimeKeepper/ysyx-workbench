package  BASYS

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

class ps2mouse extends BlackBox {
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val reset = Input(Bool())
        val ps2_clk = Analog(1.W)
        val ps2_data = Analog(1.W)
        val REn = Output(Bool())
        val mouse_data = Output(UInt(24.W))
    })
}

class ps2mouse_sim_helper extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle{
        val ren = Input(Bool())
        val data = Output(UInt(24.W))
    })
    val code: String = """module ps2mouse_sim_helper(
    |    input ren,
    |    output [23:0] data
    |);
    |
    |import "DPI-C" function void mouse_sim(output int data);
    |always @(posedge ren) begin
    |   mouse_sim({8'h00, data});
    |end
    |endmodule
    """

    setInline("mouse_helper.v", code.stripMargin)
}

class ps2mouse_sim extends Module{
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val reset = Input(Bool())
        val ps2_clk = Analog(1.W)
        val ps2_data = Analog(1.W)
        val REn = Output(Bool())
        val mouse_data = Output(UInt(24.W))
    })

    val count = RegInit(0.U(10.W))
    count := count + 1.U

    val helper = Module(new ps2mouse_sim_helper)

    when(count === 0.U){
        io.REn := true.B
        helper.io.ren := true.B
    }.otherwise{
        io.REn := false.B
        helper.io.ren := false.B
    }

    io.mouse_data := helper.io.data
}

class mouse_pointer extends Module{
    val io = IO(new Bundle{
        val ps2_clk = Analog(1.W)
        val ps2_data = Analog(1.W)

        val Left_click = Output(Bool())
        val Right_click = Output(Bool())
        val mouse_xpos = Output(UInt(11.W))
        val mouse_ypos = Output(UInt(10.W))
    })

    val ps2_mouse = Module(new ps2mouse)
    ps2_mouse.io.clock := clock
    ps2_mouse.io.reset := reset
    ps2_mouse.io.ps2_clk <> io.ps2_clk
    ps2_mouse.io.ps2_data <> io.ps2_data

    def left_click_data = ps2_mouse.io.mouse_data(0)
    def right_click_data = ps2_mouse.io.mouse_data(1)

    io.Left_click := (~RegNext(left_click_data) & left_click_data)
    io.Right_click := (~RegNext(right_click_data) & right_click_data)

    val reg_xpos = RegInit(0.S(11.W))
    val reg_ypos = RegInit(0.S(10.W))

    val next_xpos = reg_xpos + ps2_mouse.io.mouse_data(15, 8).asSInt
    val next_ypos = reg_ypos - ps2_mouse.io.mouse_data(23, 16).asSInt

    def x_bias = vga_param.vga_width / 2
    def y_bias = vga_param.vga_height / 2

    when(ps2_mouse.io.REn){
        reg_xpos := Mux(next_xpos >= x_bias.S, x_bias.S, Mux(next_xpos <= -x_bias.S, -x_bias.S, next_xpos))
        reg_ypos := Mux(next_ypos >= y_bias.S, y_bias.S, Mux(next_ypos <= -y_bias.S, -y_bias.S, next_ypos))
    }

    io.mouse_xpos := (reg_xpos + 320.S).asUInt
    io.mouse_ypos := (reg_ypos + 240.S).asUInt
}
