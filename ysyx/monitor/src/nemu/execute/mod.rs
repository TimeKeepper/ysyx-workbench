use crate::simulator;

pub struct Executor {
    pub GPR: [u32; 32],
    pub CSR: [u32; 4096],
    pub PC: u32,
}

impl Executor {
    pub fn new(ResetVector: u32) -> Self {
        Self {
            GPR: [0; 32],
            CSR: [0; 4096],
            PC: ResetVector,
        }
    }

    pub fn execute(&mut self, inst: &str) -> Result<String, simulator::SimulatorError> {
        Ok(format!("Execute: {}", inst))
    }
}