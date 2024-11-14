package BASYS

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

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
        class BRAM_ui_sim       extends BRAM_sim(width_addr = addr_width, width_data = 12, "ui")
        class BRAM_pointer_sim  extends BRAM_sim(width_addr = addr_width, width_data = 12, "pointer")

        val bram = name match {
            case "ui" => Module(new BRAM_ui_sim)
            case "pointer" => Module(new BRAM_pointer_sim)
        }

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

class img_pointer extends Module{
    val io = IO(new Bundle{
        val vgaCtrl = Input(new VGACtrlIO)

        val hit = Output(Bool())
        val rgb = Output(UInt(12.W))

        val ps2_clk = Analog(1.W)
        val ps2_data = Analog(1.W)

        val Left_click = Output(Bool())
        val Right_click = Output(Bool())
        val mouse_xpos = Output(UInt(11.W))
        val mouse_ypos = Output(UInt(10.W))
    })

    val mouse_pointer = Module(new mouse_pointer)
    mouse_pointer.io.ps2_clk <> io.ps2_clk
    mouse_pointer.io.ps2_data <> io.ps2_data

    mouse_pointer.io.Left_click <> io.Left_click
    mouse_pointer.io.Right_click <> io.Right_click
    mouse_pointer.io.mouse_xpos <> io.mouse_xpos
    mouse_pointer.io.mouse_ypos <> io.mouse_ypos

    val img_p = Module(new image(32, 32, "pointer"))
    img_p.io.pos_x := mouse_pointer.io.mouse_xpos
    img_p.io.pos_y := mouse_pointer.io.mouse_ypos
    img_p.io.vgaCtrl := io.vgaCtrl
    img_p.io.ena   := true.B

    io.hit := img_p.io.hit
    io.rgb := img_p.io.rgb
}
