use crate::{mmu::MMU, simulator};

ysyx_macro::mod_pub!(decode, execute);

pub struct Simulator {
    pub decode: decode::Decoder,
    pub execute: execute::Executor,

    pub mmu: MMU,
}

impl Simulator{
    pub fn new() -> Self {
        let mut mmu = MMU::new();
        mmu.add_memory("sdram", 0x8000_0000, 0x0800_0000);

        Self {
            decode: decode::Decoder::new(),
            execute: execute::Executor::new(0x8000_0000),

            mmu,
        }
    }
}

impl simulator::Simulator for Simulator {
    fn single_instruction(&mut self, time: u32) -> Result<String, simulator::SimulatorError> {
        for _ in 0..time {
            let addr = self.execute.PC;
            let inst = self.mmu.read(addr)?;
            let inst = self.decode.decode(inst)?;
            self.execute.execute(&inst)?;
        }
        Ok("".to_string())
    }
}
