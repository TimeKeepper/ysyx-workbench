
#[derive(Debug, PartialEq, Clone)]
pub enum SimulatorError {
    NotImplemented,
    NoMatchingMemoryByAddress  {addr: u32},
    NoMatchingMemoryByName {name: String},
    InstrctionDecodeFailed,
}

pub trait Simulator {
    fn single_instruction(&mut self, time: u32) -> Result<String, SimulatorError>;
}