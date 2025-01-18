use crate::mmu::Mask;

use super::{ExecuteInst, Simulator};

use super::super::simErr;

impl Simulator {
    pub fn execute(&mut self, inst: ExecuteInst) -> Result<(), simErr> {
        let name: &str = &inst.name;
        let mut npc = self.cpu_state.pc.value + 4;
        let gpr = &mut self.cpu_state.gpr;
        let pc = &mut self.cpu_state.pc;

        let rd = inst.rd as usize;
        let rs1 = inst.rs1 as usize;
        let rs2 = inst.rs2 as usize;
        let imm = inst.imm;
        
        match name {
            "lui" => {
                gpr[rd].value = inst.imm;
            },
            "auipc" => {
                gpr[rd].value = pc.value + inst.imm;
            },

            "jal" => {
                gpr[rd].value = npc;
                npc = pc.value.wrapping_add(inst.imm);
            },
            "jalr" => {
                gpr[rd].value = pc.value;
                npc = gpr[rs1].value.wrapping_add(inst.imm) & !1;
            },

            "sb" => {
                self.mmu.write(gpr[rs1].value + imm, gpr[rs2].value, Mask::Byte)?
            },
            "sh" => {
                self.mmu.write(gpr[rs1].value + imm, gpr[rs2].value, Mask::Half)?
            }
            "sw" => {
                self.mmu.write(gpr[rs1].value + imm, gpr[rs2].value, Mask::Word)?
            }

            "addi" => {
                gpr[rd].value = gpr[rs1].value.wrapping_add(inst.imm);
            },
            _ => return Err(simErr::InstrctionExecuteFailed { name: name.to_string() }),
        }

        pc.value = npc;

        if self.inst_trace_buffer.0 {
            self.inst_trace_buffer.1.push(inst);
        }
        
        Ok(())
    }
}