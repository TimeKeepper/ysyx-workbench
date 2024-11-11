package BASYS

import chisel3._
import chisel3.util._

class II_final extends Module{
    val io = IO(new Bundle{
        val vga_sync = new VGASyncIO
        val rgb = Output(UInt(12.W))
    })

    val clock_devider = Module(new clock_devider(4))
    clock_devider.io.clk_in := clock
    clock_devider.io.reset := reset
    
    val vga_sync = Module(new vga_sync)
    vga_sync.clock := clock_devider.io.clk_out.asClock
    vga_sync.reset := reset

    io.vga_sync <> vga_sync.io
    
    val BRAM_raddr = WireDefault((vga_sync.xaddr - 220.U) + ((vga_sync.yaddr - 140.U) * 200.U))
    val BRAM = Module(new BRAM_sim(16, 12, "ui"))
    BRAM.io.clock := clock
    BRAM.io.en  := true.B
    BRAM.io.addr  := BRAM_raddr

    io.rgb := BRAM.io.dout 
}
