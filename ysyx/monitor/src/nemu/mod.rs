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
    fn single_instruction(&mut self, time: u32) -> Result<simulator::SimulatorOk, simulator::SimulatorError> {
        for _ in 0..time {
            let addr = self.execute.PC;
            let inst = self.mmu.read(addr)?;
            let inst = self.decode.decode(inst)?;
            self.execute.execute(inst)?;
        }
        Ok(simulator::SimulatorOk::InstructionExecuted)
    }
}

pub struct ExecuteInst {
    pub name: String,
    pub rs1: u8,
    pub rs2: u8,
    pub rd: u8,
    pub imm: u32,
}

impl ExecuteInst {
    pub fn new(name: &str, rs1: u8, rs2: u8, rd: u8, imm: u32) -> Self {
        Self {
            name: name.to_string(),
            rs1,
            rs2,
            rd,
            imm,
        }
    }
}
