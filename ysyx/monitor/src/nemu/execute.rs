use super::{ExecuteInst, Simulator};

use super::super::simErr;

impl Simulator {
    pub fn execute(&mut self, inst: ExecuteInst) -> Result<(), simErr> {
        let name: &str = &inst.name;
        let mut npc = self.cpu_state.pc.value + 4;
        let gpr = &mut self.cpu_state.gpr;
        let pc = &mut self.cpu_state.pc;
        
        match name {
            "lui" => {
                gpr[inst.rd as usize].value = inst.imm;
            },
            "auipc" => {
                gpr[inst.rd as usize].value = pc.value + inst.imm;
            },

            "jal" => {
                gpr[inst.rd as usize].value = npc;
                npc = pc.value.wrapping_add(inst.imm);
            },
            "jalr" => {
                gpr[inst.rd as usize].value = pc.value;
                npc = gpr[inst.rs1 as usize].value.wrapping_add(inst.imm) & !1;
            },

            "sb" => {
            },

            "addi" => {
                gpr[inst.rd as usize].value = gpr[inst.rs1 as usize].value.wrapping_add(inst.imm);
            },
            _ => return Err(simErr::UnknownInstruction { name: name.to_string() }),
        }
        
        pc.value = npc;

        if self.inst_trace_buffer.0 {
            self.inst_trace_buffer.1.push(inst);
        }
        
        Ok(())
    }
}