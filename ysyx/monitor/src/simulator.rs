
#[derive(Debug, PartialEq, Clone)]
pub enum SimulatorError {
    NotImplemented,
    NoMatchingMemory,
    InstrctionDecodeFailed,
}

pub trait Simulator {
    fn single_instruction(&mut self, time: u32) -> Result<String, SimulatorError>;
}