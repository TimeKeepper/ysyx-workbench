use crate::mmu::MatchMsg;

#[derive(Debug, PartialEq, Clone)]
pub enum SimulatorOk {
    Nothing,
    InstructionExecuted,
}

#[derive(Debug, PartialEq, Clone)]
pub enum SimulatorError {
    NotImplemented,
    InvalidCommand,
    NoBinaryFile,
    DiffertestFailed,
    BinaryFileNotFound,
    NoMatchingMemory {msg: MatchMsg},
    InstrctionDecodeFailed {inst: u32},
    InstrctionExecuteFailed {name: String},
}

pub trait Simulator {
    fn single_instruction(&mut self) -> Result<SimulatorOk, SimulatorError>;
}

#[derive(Debug, PartialEq, Clone)]
pub struct Register {
    pub name: &'static str,
    pub value: u32,
}

impl Register {
    pub fn new(name: &'static str, value: u32) -> Self {
        Self {
            name,
            value,
        }
    }
}