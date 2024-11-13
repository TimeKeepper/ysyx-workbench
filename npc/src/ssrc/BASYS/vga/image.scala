package BASYS

import chisel3._
import chisel3.util._
import os.stat.posix

class image(width: Int, height: Int, name: String) extends Module{
    val io = IO(new Bundle{
        val vgaCtrl = Input(new VGACtrlIO)
        val pos_x = Input(UInt())
        val pos_y = Input(UInt())
        val ena   = Input(Bool())
        val hit = Output(Bool())
        val rgb = Output(UInt(12.W))
    })
    
    val (vga_match, raddr) = BASYS_utils.pos_match(io.pos_x, io.pos_y, width.U, height.U, io.vgaCtrl.xaddr, io.vgaCtrl.yaddr, io.ena & io.vgaCtrl.valid)

    val addr_width = log2Ceil(width * height)

    io.hit := vga_match

    if (sync_config.sim == true) {
        val bram = Module(new BRAM_sim(17, 12, name))
        bram.io.clka := clock
        bram.io.ena  := vga_match
        bram.io.addra  := raddr
        io.rgb := bram.io.douta
    } else {
        class BRAM_framea   extends BRAM(width_addr = addr_width, width_data = 12)
        class BRAM_frameb   extends BRAM(width_addr = addr_width, width_data = 12)
        class BRAM_ui       extends BRAM(width_addr = addr_width, width_data = 12)
        class BRAM_pointer  extends BRAM(width_addr = addr_width, width_data = 12)

        val bram = name match {
            case "frame_a" => Module(new BRAM_framea)
            case "frame_b" => Module(new BRAM_frameb)
            case "ui" => Module(new BRAM_ui)
            case "pointer" => Module(new BRAM_pointer)
            case _ => Module(new BRAM(16, 12))
        }
        
        bram.io.clka := clock
        bram.io.ena  := vga_match
        bram.io.addra  := raddr
        io.rgb := bram.io.douta
    }
}
