package BASYS

import chisel3._
import chisel3.util._

class II_final extends Module{
    val io = IO(new Bundle{
        val vga_sync = new VGASyncIO
        val rgb = Output(UInt(12.W))
    })
    
    val vga_sync = Module(new vga_sync)

    io.vga_sync <> vga_sync.io
    
    val img_ui = Module(new image(200, 200, "ui", 220, 140))

    img_ui.io.xaddr := vga_sync.xaddr
    img_ui.io.yaddr := vga_sync.yaddr

    when(img_ui.io.hit){
        io.rgb := img_ui.io.rgb
    }.otherwise{
        io.rgb := "hfff".U
    }
}
