package BASYS

import chisel3._
import chisel3.util._

class image(width: Int, height: Int, name: String, pos_x: Int, pos_y: Int) extends Module{
    val io = IO(new Bundle{
        val xaddr = Input(UInt(10.W))
        val yaddr = Input(UInt(10.W))
        val hit = Output(Bool())
        val rgb = Output(UInt(12.W))
    })

    def bias_x = pos_x
    def bias_y = pos_y
    
    val x_hit = WireDefault(io.xaddr >= (bias_x).U && io.xaddr < (bias_x + width).U)
    val y_hit = WireDefault(io.yaddr >= (bias_y).U && io.yaddr < (bias_y + height).U)
    io.hit := RegNext(x_hit && y_hit)

    val b_xaddr = WireDefault(io.xaddr - (bias_x).U)
    val b_yaddr = WireDefault(io.yaddr - (bias_y).U)
    
    val BRAM_raddr = WireDefault(((b_xaddr >> 0.U) + ((b_yaddr >> 0.U) * (width >> 0).U)))

    if (sync_config.sim == true) {
        val bram = Module(new BRAM_sim(17, 12, name))
        bram.io.clka := clock
        bram.io.ena  := io.hit
        bram.io.addra  := BRAM_raddr
        io.rgb := RegNext(bram.io.douta)
    } else {
        class BRAM_ui extends BRAM(width_addr = 16, width_data = 12)
        class BRAM_Pointer extends BRAM(width_addr = 9, width_data = 12)

        val bram = name match {
            case "ui" => Module(new BRAM_ui)
            case "pointer" => Module(new BRAM_Pointer)
        }
        
        bram.io.clka := clock
        bram.io.ena  := io.hit
        bram.io.addra  := BRAM_raddr
        io.rgb := RegNext(bram.io.douta)
    }
}
