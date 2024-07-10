package ram

import chisel3._
import chisel3.util._

//此模块将32为数据读取并根据memop处理数据，延迟一个周期后发送给cpu

class SRAM extends Module {
    val io = IO(new Bundle{
        val inst_input = Flipped(Decoupled(UInt(32.W)))
        val inst_output = Decoupled(UInt(32.W))
    })
    
    val inst_cache = RegInit(UInt(32.W), "h0".U)

    val s_wait_valid :: s_wait_ready :: s_busy :: Nil = Enum(3)
    val state = RegInit(s_wait_valid)

    when(io.inst_input.valid && io.inst_input.ready) {
        inst_cache := io.inst_input.bits
    }
    
    io.inst_output.bits := inst_cache
    
    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.inst_output.valid, s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.inst_output.ready, s_wait_valid, s_wait_ready)
        )
    )

    io.inst_output.valid := io.inst_input.valid
    io.inst_input.ready  := state === s_wait_valid
}