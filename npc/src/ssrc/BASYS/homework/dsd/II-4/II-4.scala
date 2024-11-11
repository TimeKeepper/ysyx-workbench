package BASYS

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

class II_4 extends Module {
    val io = IO(new Bundle{
        val hsync, vsync = Output(Bool())
        val rgb = Output(UInt(12.W))
        val ps2_clk = Analog(1.W)
        val ps2_data = Analog(1.W)
    })

    val vga_sync = Module(new VGA_SYNC)
    vga_sync.io.clock := clock
    vga_sync.io.reset := reset
    io.hsync := vga_sync.io.hsync
    io.vsync := vga_sync.io.vsync

    val ps2_mouse = Module(new ps2mouse)
    ps2_mouse.io.clock := clock
    ps2_mouse.io.reset := reset
    ps2_mouse.io.ps2_clk <> io.ps2_clk
    ps2_mouse.io.ps2_data <> io.ps2_data

    val left_click_cache = RegNext(ps2_mouse.io.mouse_data(0))
    val right_click_cache = RegNext(ps2_mouse.io.mouse_data(1))
    val left_click = WireDefault(!left_click_cache && ps2_mouse.io.mouse_data(0))
    val right_click = WireDefault(!right_click_cache && ps2_mouse.io.mouse_data(1))

    val mouse_x = RegInit(0.S(11.W))
    val mouse_y = RegInit(0.S(10.W))

    val Next_x = mouse_x + ps2_mouse.io.mouse_data(15, 8).asSInt
    val Next_y = mouse_y - ps2_mouse.io.mouse_data(23, 16).asSInt

    when(ps2_mouse.io.REn){
        mouse_x := Mux(Next_x > 320.S, 320.S, Mux(Next_x < -320.S, -320.S, Next_x))
        mouse_y := Mux(Next_y > 240.S, 240.S, Mux(Next_y < -240.S, -240.S, Next_y))
    }

    val vga_match = WireDefault(vga_sync.io.x >= 220.U && vga_sync.io.x < 420.U && vga_sync.io.y >= 140.U && vga_sync.io.y < 340.U)
    val vga_match_f = WireDefault(vga_sync.io.x >= 70.U && vga_sync.io.x < 270.U && vga_sync.io.y >= 140.U && vga_sync.io.y < 340.U)
    val vga_match_s = WireDefault(vga_sync.io.x >= 370.U && vga_sync.io.x < 570.U && vga_sync.io.y >= 140.U && vga_sync.io.y < 340.U)

    val mouse_match_f = WireDefault(mouse_x >= -250.S && mouse_x < -50.S && mouse_y >= -100.S && mouse_y < 100.S)
    val mouse_match_s = WireDefault(mouse_x >= 50.S && mouse_x < 250.S && mouse_y >= -100.S && mouse_y < 100.S)

    val s_idle :: s_frame_f :: s_frame_s :: Nil = Enum(3)

    val state = RegInit(s_frame_f)
    state := MuxLookup(state, s_frame_f)(Seq(
        s_idle -> Mux(left_click, Mux(mouse_match_f, s_frame_f, Mux(mouse_match_s, s_frame_s, s_idle)), s_idle),
        s_frame_f -> Mux(right_click, s_idle, s_frame_f),
        s_frame_s -> Mux(right_click, s_idle, s_frame_s)
    ))

    val BRAM_raddr = WireDefault((vga_sync.io.x - 220.U) + ((vga_sync.io.y - 140.U) * 200.U))
    val BRAM_0_raddr = WireDefault((vga_sync.io.x - 220.U) + ((vga_sync.io.y - 140.U) * 200.U))

    when(state === s_idle){
        BRAM_raddr := (vga_sync.io.x - 70.U) + ((vga_sync.io.y - 140.U) * 200.U)
        BRAM_0_raddr := (vga_sync.io.x - 370.U) + ((vga_sync.io.y - 140.U) * 200.U)
    }

    val BRAM = Module(new BRAM(16, 12))
    BRAM.io.clka := clock
    BRAM.io.ena := true.B
    BRAM.io.addra := BRAM_raddr

    val BRAM_0 = Module(new BRAM_0(16, 12))
    BRAM_0.io.clka := clock
    BRAM_0.io.ena := true.B
    BRAM_0.io.addra := BRAM_0_raddr

    val BRAM_P = Module(new BRAM_P)
    BRAM_P.io.clka := clock
    BRAM_P.io.ena := true.B
    BRAM_P.io.addra := (vga_sync.io.x - (mouse_x + 320.S).asUInt) + (vga_sync.io.y - (mouse_y + 240.S).asUInt) * 20.U

    val frame = WireDefault(0.U(12.W))
    when((state === s_frame_f) & vga_match) {
        frame := BRAM.io.douta
    }.elsewhen((state === s_frame_s) & vga_match){
        frame := BRAM_0.io.douta
    }.elsewhen((state === s_idle) & vga_match_f){
        frame := BRAM.io.douta
    }.elsewhen((state === s_idle) & vga_match_s){
        frame := BRAM_0.io.douta
    }

    when(vga_sync.io.x - (mouse_x + 320.S).asUInt < 20.U && vga_sync.io.y - (mouse_y + 240.S).asUInt < 20.U && vga_sync.io.x - (mouse_x + 320.S).asUInt >= 0.U && vga_sync.io.y - (mouse_y + 240.S).asUInt >= 0.U){
        io.rgb := BRAM_P.io.douta
    }.otherwise{
        io.rgb := frame
    }
}
