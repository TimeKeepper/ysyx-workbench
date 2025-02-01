use super::super::mmu::MMU;
use circular_queue::CircularQueue;

pub struct Simulator {
    pub mmu: MMU,
    
    pub inst_trace_buffer: (bool, CircularQueue<u32>),
    pub inst_trace: bool,
    pub execte_times: (u32, u32),
}

impl Simulator {
    pub fn new() -> Self {
        let mut mmu = MMU::new();
        mmu.add_memory("sram",  0x0f00_0000, 0x0000_2000);
        mmu.add_memory("mrom",  0x2000_0000, 0x0000_1000);
        mmu.add_memory("flash", 0x3000_0000, 0x1000_0000);
        mmu.add_memory("psram", 0x8000_0000, 0x0800_0000);
        mmu.add_memory("sdram", 0xa000_0000, 0x0200_0000);

        Self {
            mmu,
            inst_trace_buffer: (false, CircularQueue::with_capacity(10)),
            inst_trace: false,
            execte_times: (0, 0),
        }
    }
}
