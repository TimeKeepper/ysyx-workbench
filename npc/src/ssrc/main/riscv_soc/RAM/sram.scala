package ram

import chisel3._
import chisel3.util._

//此模块将32为数据读取并根据memop处理数据，延迟一个周期后发送给cpu

class SRAM extends Module {
    val io = IO(new Bundle{
        val inst_input = Input(UInt(32.W))
        val inst_output = Decoupled(UInt(32.W))
    })
    
    val inst_cache = RegInit(UInt(32.W), "h0".U)

    val s_idle :: s_wait_ready :: Nil = Enum(2)
    val state = RegInit(s_idle)

    inst_cache := io.inst_input
    
    io.inst_output.bits := inst_cache
    
    state := MuxLookup(state, s_idle)(
        Seq(
            s_idle       -> Mux(io.inst_output.valid, s_wait_ready, s_idle),
            s_wait_ready -> Mux(io.inst_output.ready, s_idle, s_wait_ready)
        )
    )

    io.inst_output.valid := (state === s_idle)
}