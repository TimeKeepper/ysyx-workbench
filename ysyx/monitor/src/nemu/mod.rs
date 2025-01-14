use crate::{mmu::MMU, simulator};

ysyx_macro::mod_pub!(decode, execute);
use circular_queue::CircularQueue;
pub struct Simulator {
    pub decoder: decode::Decoder,
    pub executer: execute::Executor,

    pub mmu: MMU,

    pub inst_trace_buffer: (bool, CircularQueue<ExecuteInst>),
    pub inst_trace: bool
}

impl Simulator{
    pub fn new() -> Self {
        let mut mmu = MMU::new();
        mmu.add_memory("sdram", 0x8000_0000, 0x0800_0000);

        Self {
            decoder: decode::Decoder::new(),
            executer: execute::Executor::new(0x8000_0000),

            mmu,
            inst_trace_buffer: (false, CircularQueue::with_capacity(10)),
            inst_trace: false,
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

    fn execute(&mut self, inst: ExecuteInst) -> Result<(), simulator::SimulatorError> {
        self.executer.execute(inst.clone())?;

        if self.inst_trace {
            println!("{:?}", inst);
        }

        if self.inst_trace_buffer.0 {
            self.inst_trace_buffer.1.push(inst.clone());
        }
        
        Ok(())
    }
}

impl simulator::Simulator for Simulator {
    fn single_instruction(&mut self, time: u32) -> Result<simulator::SimulatorOk, simulator::SimulatorError> {
        for _ in 0..time {
            let addr = self.executer.pc;
            let inst = self.mmu.read(addr)?;
            let inst = self.decoder.decode(inst)?;
            self.execute(inst)?;
        }
        Ok(simulator::SimulatorOk::InstructionExecuted)
    }
}

#[derive(Debug, PartialEq, Clone)]
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
