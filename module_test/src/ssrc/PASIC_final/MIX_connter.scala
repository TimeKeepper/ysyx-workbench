package Mixer

import chisel3._
import chisel3.util._

class Mixer(val rows: Int, val cols: Int) extends Module {
    val RAM1 = Mem(rows, Vec(cols, Bool()))
    val RAM2 = Mem(rows, Vec(cols, Bool()))

    
}