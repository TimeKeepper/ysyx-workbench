package BASYS

import chisel3._
import chisel3.util._

class II_3 extends Module {
    val io = IO(new Bundle{
        val hsync, vsync = Output(Bool())
        val rgb = Output(UInt(12.W))
        val sw  = Input(Bool())
    })
    val vga_sync = Module(new VGA_SYNC)
    vga_sync.io.clock := clock
    vga_sync.io.reset := reset
    io.hsync := vga_sync.io.hsync
    io.vsync := vga_sync.io.vsync

    val key = Module(new key)
    key.io.key_in := io.sw

    val s_raw :: s_pitch :: Nil = Enum(2)
    val state = RegInit(s_raw)

    state := MuxLookup(state, s_raw)(Seq(
        s_raw -> Mux(key.io.is_key_posedge, s_pitch, s_raw),
        s_pitch -> Mux(key.io.is_key_posedge, s_raw, s_pitch)
    ))

    when(Mux(state === s_raw, vga_sync.io.x < 80.U, vga_sync.io.y < 60.U)){
        io.rgb := "hF00".U
    }.elsewhen(Mux(state === s_raw, vga_sync.io.x < 160.U, vga_sync.io.y < 120.U)){
        io.rgb := "h00F".U
    }.elsewhen(Mux(state === s_raw, vga_sync.io.x < 240.U, vga_sync.io.y < 180.U)){
        io.rgb := "h0F0".U
    }.elsewhen(Mux(state === s_raw, vga_sync.io.x < 320.U, vga_sync.io.y < 240.U)){
        io.rgb := "h00F".U
    }.elsewhen(Mux(state === s_raw, vga_sync.io.x < 400.U, vga_sync.io.y < 300.U)){
        io.rgb := "hF00".U
    }.elsewhen(Mux(state === s_raw, vga_sync.io.x < 480.U, vga_sync.io.y < 360.U)){
        io.rgb := "h0F0".U
    }.elsewhen(Mux(state === s_raw, vga_sync.io.x < 560.U, vga_sync.io.y < 420.U)){
        io.rgb := "hF00".U
    }.otherwise{
        io.rgb := "h00F".U
    }
}
