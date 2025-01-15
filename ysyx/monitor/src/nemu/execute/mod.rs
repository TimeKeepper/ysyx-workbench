use crate::simulator;

use super::ExecuteInst;
use super::simulator::Register;
pub struct Executor {
    pub gpr: [Register; 32],
    pub csr: [Register; 4096],
    pub pc: Register,
}

impl Executor {
    pub fn new(reset_vector: u32) -> Self {
        let gpr_name_list = [
            "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
            "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
            "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
            "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
        ];

        Self {
            gpr: core::array::from_fn(|i| Register::new(gpr_name_list[i], 0)),
            csr: core::array::from_fn(|_| Register::new("csr", 0)),
            pc: Register::new("pc", reset_vector),
        }
    }

    pub fn execute(&mut self, inst: ExecuteInst) -> Result<(), simulator::SimulatorError> {
        let name: &str = &inst.name;
        let mut npc = self.pc.value + 4;
        match name {
            "lui" => {
                self.gpr[inst.rd as usize].value = inst.imm;
            },
            "auipc" => {
                self.gpr[inst.rd as usize].value = self.pc.value + inst.imm;
            },

            "jal" => {
                self.gpr[inst.rd as usize].value = self.pc.value;
                npc = self.pc.value.wrapping_add(inst.imm);
            },
            "jalr" => {
                self.gpr[inst.rd as usize].value = self.pc.value;
                npc = self.gpr[inst.rs1 as usize].value.wrapping_add(inst.imm) & !1;
            },

            "addi" => {
                self.gpr[inst.rd as usize].value = self.gpr[inst.rs1 as usize].value.wrapping_add(inst.imm);
            },
            _ => return Err(simulator::SimulatorError::UnknownInstruction { name: name.to_string() }),
        }
        self.pc.value = npc;
        Ok(())
    }
}