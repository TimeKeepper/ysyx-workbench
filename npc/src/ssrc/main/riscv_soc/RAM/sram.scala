package ram

import chisel3._
import chisel3.util._

class Icache_input extends Bundle{
    val inst = Input(UInt(32.W))
    val addr = Input(UInt(32.W))
}

class Icache_output extends Bundle{
    val inst = Output(UInt(32.W))
    val addr = Output(UInt(32.W))
}

//此模块将32为数据读取并根据memop处理数据，延迟一个周期后发送给cpu

class Icache extends Module {
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(new Icache_input))
        val inst_output = Decoupled(UInt(32.W))
    })
    
    val inst_cache = RegInit(UInt(32.W), "h0".U)

    val s_wait_valid :: s_wait_ready :: s_busy :: Nil = Enum(3)
    val state = RegInit(s_wait_valid)

    when(io.in.valid && io.in.ready) {
        inst_cache := io.in.bits.inst
    }
    
    io.inst_output.bits := inst_cache
    
    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.inst_output.valid, s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.inst_output.ready, s_wait_valid, s_wait_ready)
        )
    )

    io.inst_output.valid := io.in.valid
    io.in.ready  := state === s_wait_valid
}