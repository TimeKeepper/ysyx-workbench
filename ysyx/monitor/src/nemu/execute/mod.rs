use crate::simulator;

use super::ExecuteInst;

struct RiscvInst {
    pub rd: u8,
    pub rs1: u8,
    pub rs2: u8,
    pub imm: u32,
    pub csr: u32,

    pub name: String,
}

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

    pub fn execute(&mut self, inst: ExecuteInst) -> Result<String, simulator::SimulatorError> {
        Ok(format!("Execute: {}", inst.name))
    }
}