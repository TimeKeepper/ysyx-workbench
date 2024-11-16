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

    def ui_posx = 170
    def ui_posy = 90

    val img_ui = Module(new image(300, 300, "ui"))
    img_ui.io.pos_x := ui_posx.U
    img_ui.io.pos_y := ui_posy.U
    img_ui.io.vgaCtrl := vga_sync.Ctrl
    img_ui.io.ena   := true.B

    val button_select = WireDefault(0.U(6.W))

    val hitStatePairs = Seq(
        BASYS_utils.pos_hit((ui_posx + 125).U, (ui_posy + 245).U, 47.U, 25.U, img_pointer.io.mouse_xpos, img_pointer.io.mouse_ypos, true.B) -> 32.U,
        BASYS_utils.pos_hit((ui_posx + 180).U, (ui_posy + 245).U, 47.U, 25.U, img_pointer.io.mouse_xpos, img_pointer.io.mouse_ypos, true.B) -> 16.U,
        BASYS_utils.pos_hit((ui_posx + 230).U, (ui_posy + 245).U, 50.U, 25.U, img_pointer.io.mouse_xpos, img_pointer.io.mouse_ypos, true.B) -> 8.U,
        BASYS_utils.pos_hit(200.U, 240.U, 64.U, 40.U, img_pointer.io.mouse_xpos, img_pointer.io.mouse_ypos, true.B) -> 4.U,
        BASYS_utils.pos_hit(290.U, 240.U, 64.U, 40.U, img_pointer.io.mouse_xpos, img_pointer.io.mouse_ypos, true.B) -> 2.U,
        BASYS_utils.pos_hit(380.U, 240.U, 64.U, 40.U, img_pointer.io.mouse_xpos, img_pointer.io.mouse_ypos, true.B) -> 1.U
    )

    button_select := Mux1H(hitStatePairs)

    val img_button = Module(new img_button)
    img_button.io.pos_x := 380.U
    img_button.io.pos_y := 240.U
    img_button.io.vgaCtrl := vga_sync.Ctrl
    img_button.io.ena   := true.B
    img_button.io.state    := button_select(2, 0)

    val num = RegInit(0.U(14.W))

    when(img_pointer.io.Left_click){
        num := MuxLookup(button_select, num)(Seq(
            32.U -> (num + 5.U),
            16.U -> (num + 10.U),
            8.U -> (num + 100.U),
            4.U -> Mux(num >= 10.U, (num - 10.U), num),
            2.U -> Mux(num >= 25.U, (num - 25.U), num),
            1.U -> Mux(num >= 40.U, (num - 40.U), num),
        ))
    }

    val img_num = Module(new img_number)
    img_num.io.pos_x := 200.U
    img_num.io.pos_y := 330.U
    img_num.io.vgaCtrl := vga_sync.Ctrl
    img_num.io.ena   := true.B
    img_num.io.number := num

    when(img_pointer.io.hit){
        io.rgb := img_pointer.io.rgb
    }.elsewhen(img_button.io.hit){
        io.rgb := img_button.io.rgb
    }.elsewhen(img_num.io.hit){
        io.rgb := img_num.io.rgb
    }.elsewhen(img_ui.io.hit){
        io.rgb := img_ui.io.rgb
    }.elsewhen(window_match){
        io.rgb := "hfff".U
    }.otherwise{
        io.rgb := "h000".U
    }
}
