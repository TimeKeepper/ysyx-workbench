use crate::simulator;

use super::ExecuteInst;

pub struct Executor {
    pub gpr: [u32; 32],
    pub csr: [u32; 4096],
    pub pc: u32,
}

impl Executor {
    pub fn new(reset_vector: u32) -> Self {
        Self {
            gpr: [0; 32],
            csr: [0; 4096],
            pc: reset_vector,
        }
    }

    pub fn execute(&mut self, inst: ExecuteInst) -> Result<(), simulator::SimulatorError> {
        let name: &str = &inst.name;
        let mut npc = self.pc + 4;
        match name {
            "lui" => {
                self.gpr[inst.rd as usize] = inst.imm;
            },
            "auipc" => {
                self.gpr[inst.rd as usize] = self.pc + inst.imm;
            },

            "jal" => {
                self.gpr[inst.rd as usize] = self.pc;
                npc = self.pc.wrapping_add(inst.imm);
            },
            "jalr" => {
                self.gpr[inst.rd as usize] = self.pc;
                npc = self.gpr[inst.rs1 as usize].wrapping_add(inst.imm) & !1;
            },

            "addi" => {
                self.gpr[inst.rd as usize] = self.gpr[inst.rs1 as usize].wrapping_add(inst.imm);
            },
            _ => return Err(simulator::SimulatorError::UnknownInstruction { name: name.to_string() }),
        }
        self.pc = npc;
        Ok(())
    }
}