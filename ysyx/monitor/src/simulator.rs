
#[derive(Debug, PartialEq, Clone)]
pub enum SimulatorOk {
    InstructionExecuted,
}

#[derive(Debug, PartialEq, Clone)]
pub enum SimulatorError {
    NotImplemented,
    NoBinaryFile,
    BinaryFileNotFound,
    NoMatchingMemoryByAddress  {addr: u32},
    NoMatchingMemoryByName {name: String},
    InstrctionDecodeFailed,
}

pub trait Simulator {
    fn single_instruction(&mut self, time: u32) -> Result<SimulatorOk, SimulatorError>;
}