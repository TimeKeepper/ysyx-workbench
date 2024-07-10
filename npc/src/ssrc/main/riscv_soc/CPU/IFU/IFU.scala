package riscv_cpu

import chisel3._
import chisel3.util._

class IFU_input extends Bundle{
    val inst = Input(UInt(32.W))
    val addr = Input(UInt(32.W))
}

class IFU_output extends Bundle{
    val inst = Output(UInt(32.W))
    val addr = Output(UInt(32.W))
}

//此模块将32为数据读取并根据memop处理数据，延迟一个周期后发送给cpu

class IFU extends Module {
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(new IFU_input))
        val out = Decoupled(new IFU_output)
    })

    val s_wait_valid :: s_wait_ready :: s_busy :: Nil = Enum(3)
    val state = RegInit(s_wait_ready)
    
    state := MuxLookup(state, s_wait_ready)(
        Seq(
            s_wait_valid -> Mux(io.in.valid,  s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.out.ready, s_wait_valid, s_wait_ready)
        )
    )
    
    val inst_cache = RegInit(UInt(32.W), "h0".U)
    val addr_cache = RegInit(UInt(32.W), "h80000000".U)

    when(io.in.valid && io.in.ready) {
        inst_cache := io.in.bits.inst 
        addr_cache := io.in.bits.addr
    }
    
    io.out.bits.inst := inst_cache
    io.out.bits.addr := addr_cache

    io.out.valid := state === s_wait_ready
    io.in.ready  := state === s_wait_valid
}

class Dcache_input extends Bundle{
    val addr = Input(UInt(32.W))
    val data = Input(UInt(32.W))
    val memop = Input(UInt(3.W))
    val memwen = Input(Bool())
}

class Dcache_output extends Bundle{
    val data = Output(UInt(32.W))
}

class Dcache extends Module {
    val io = IO(new Bundle{
        val in = Flipped(Decoupled(new Dcache_input))
        val out = Decoupled(new Dcache_output)
    })

    val s_wait_valid :: s_wait_ready :: s_busy :: Nil = Enum(3)
    val state = RegInit(s_wait_valid)
    
    state := MuxLookup(state, s_wait_valid)(
        Seq(
            s_wait_valid -> Mux(io.out.valid, s_wait_ready, s_wait_valid),
            s_wait_ready -> Mux(io.out.ready, s_wait_valid, s_wait_ready)
        )
    )

}