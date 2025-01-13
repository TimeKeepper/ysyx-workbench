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

    pub fn init(&mut self, bin_path: Option<String>) -> Result<(), simulator::SimulatorError> {
        if bin_path.is_none() { return Err(simulator::SimulatorError::NoBinaryFile) };
        let bin = std::fs::read(bin_path.unwrap());
        if bin.is_err() { return Err(simulator::SimulatorError::BinaryFileNotFound) };
        let bin = bin.unwrap();
        self.mmu.load("sdram", &bin).unwrap();
        return Ok(());
    }
}

impl simulator::Simulator for Simulator {
    fn single_instruction(&mut self, time: u32) -> Result<String, simulator::SimulatorError> {
        let mut result: String = String::new();
        for _ in 0..time {
            let addr = self.execute.PC;
            let inst = self.mmu.read(addr)?;
            let inst = self.decode.decode(inst)?;
            result = self.execute.execute(&inst)?;
        }
        Ok(result)
    }
}
