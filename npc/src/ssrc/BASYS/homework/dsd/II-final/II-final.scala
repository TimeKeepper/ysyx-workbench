package BASYS

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

class II_final extends Module{
    val io = IO(new Bundle{
        val sync = new VGASyncIO
        val rgb = Output(UInt(12.W))
        val ps2_clk = Analog(1.W)
        val ps2_data = Analog(1.W)
    })
    
    val vga_sync = Module(new vga_sync)

    if(sync_config.sim == true){
        val vga_valid = IO(Output(Bool()))
        vga_valid := vga_sync.Ctrl.valid
    }

    io.sync <> vga_sync.io
    
    val img_pointer = Module(new img_pointer)
    img_pointer.io.vgaCtrl := vga_sync.Ctrl
    img_pointer.io.ps2_clk <> io.ps2_clk
    img_pointer.io.ps2_data <> io.ps2_data

    val (window_match, _) = BASYS_utils.pos_match(20.U, 0.U, 600.U, 480.U, vga_sync.Ctrl.xaddr, vga_sync.Ctrl.yaddr, true.B)

    val img_ui = Module(new image(300, 300, "ui"))
    img_ui.io.pos_x := 170.U
    img_ui.io.pos_y := 90.U
    img_ui.io.vgaCtrl := vga_sync.Ctrl
    img_ui.io.ena   := true.B

    when(img_pointer.io.hit){
        io.rgb := img_pointer.io.rgb
    }.elsewhen(img_ui.io.hit){
        io.rgb := img_ui.io.rgb
    }.elsewhen(window_match){
        io.rgb := "hfff".U
    }.otherwise{
        io.rgb := "h000".U
    }
}
