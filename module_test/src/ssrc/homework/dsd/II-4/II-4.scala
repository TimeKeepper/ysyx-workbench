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

    val BRAM = Module(new BRAM(16, 12))
    BRAM.io.clka := clock
    BRAM.io.ena := true.B
    BRAM.io.addra := (vga_sync.io.x - 220.U) + ((vga_sync.io.y - 140.U) * 200.U)

    val BRAM_0 = Module(new BRAM_0(16, 12))
    BRAM_0.io.clka := clock
    BRAM_0.io.ena := true.B
    BRAM_0.io.addra := (vga_sync.io.x - 220.U) + ((vga_sync.io.y - 140.U) * 200.U)

    val ps2_mouse = Module(new ps2mouse)
    ps2_mouse.io.clock := clock
    ps2_mouse.io.reset := reset
    ps2_mouse.io.ps2_clk <> io.ps2_clk
    ps2_mouse.io.ps2_data <> io.ps2_data

    val s_frame_f :: s_frame_s :: Nil = Enum(2)

    val state = RegInit(s_frame_f)
    state := MuxLookup(state, s_frame_f)(Seq(
        s_frame_f -> Mux(ps2_mouse.io.is_left_click, s_frame_s, s_frame_f),
        s_frame_s -> Mux(ps2_mouse.io.is_left_click, s_frame_f, s_frame_s)
    ))

    when((state === s_frame_f) & (vga_sync.io.x >= 220.U && vga_sync.io.x < 420.U && vga_sync.io.y >= 140.U && vga_sync.io.y < 340.U)) {
        io.rgb := BRAM.io.douta
    }.elsewhen((state === s_frame_s) & (vga_sync.io.x >= 220.U && vga_sync.io.x < 420.U && vga_sync.io.y >= 140.U && vga_sync.io.y < 340.U)){
        io.rgb := BRAM_0.io.douta
    }.otherwise {
        io.rgb := 0.U
    }
}
