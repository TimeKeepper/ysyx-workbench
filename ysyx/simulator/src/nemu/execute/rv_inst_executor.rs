use crate::mmu::Mask;

use super::super::{ExecuteInst, Simulator, sig_extend};

use super::super::super::simErr;

#[derive(Debug, PartialEq, Clone)]
enum ExecuteResult {
    UnknownInst,
    Ok,
}

impl Simulator {
    fn rv32i_execute(&mut self, inst: &ExecuteInst) -> Result<ExecuteResult, simErr> {
        let name: &str = &inst.name;
        let rd = inst.rd as usize;
        let rs1 = inst.rs1 as usize;
        let rs2 = inst.rs2 as usize;
        let imm = inst.imm;
        
        let mut npc = self.cpu_state.pc.value + 4;
        let gpr = &mut self.cpu_state.gpr;
        let pc = &mut self.cpu_state.pc;

        match name {
            "lui" => {
                gpr[rd].value = inst.imm;
            }
            "auipc" => {
                gpr[rd].value = pc.value + inst.imm;
            }

            "jal" => {
                gpr[rd].value = npc;
                npc = pc.value.wrapping_add(inst.imm);
            }
            "jalr" => {
                gpr[rd].value = npc;
                npc = gpr[rs1].value.wrapping_add(inst.imm) & !1;
            }

            "beq" => {
                if gpr[rs1].value == gpr[rs2].value {
                    npc = pc.value.wrapping_add(inst.imm);
                }
            }
            "bne" => {
                if gpr[rs1].value != gpr[rs2].value {
                    npc = pc.value.wrapping_add(inst.imm);
                }
            }
            "blt" => {
                if (gpr[rs1].value as i32) < (gpr[rs2].value as i32) {
                    npc = pc.value.wrapping_add(inst.imm);
                }
            }
            "bge" => {
                if (gpr[rs1].value as i32) >= (gpr[rs2].value as i32) {
                    npc = pc.value.wrapping_add(inst.imm);
                }
            }
            "bltu" => {
                if gpr[rs1].value < gpr[rs2].value {
                    npc = pc.value.wrapping_add(inst.imm);
                }
            }
            "bgeu" => {
                if gpr[rs1].value >= gpr[rs2].value {
                    npc = pc.value.wrapping_add(inst.imm);
                }
            }

            "lb" => {
                gpr[rd].value = sig_extend(self.mmu.read(gpr[rs1].value + imm, Mask::Byte)?, 8);
            }
            "lh" => {
                gpr[rd].value = sig_extend(self.mmu.read(gpr[rs1].value + imm, Mask::Half)?, 16);
            }
            "lw" => {
                gpr[rd].value = self.mmu.read(gpr[rs1].value + imm, Mask::Word)?;
            }
            "lbu" => {
                gpr[rd].value = self.mmu.read(gpr[rs1].value + imm, Mask::Byte)?;
            }
            "lhu" => {
                gpr[rd].value = self.mmu.read(gpr[rs1].value + imm, Mask::Half)?;
            }

            "sb" => {
                self.mmu.write(gpr[rs1].value + imm, gpr[rs2].value, Mask::Byte)?
            }
            "sh" => {
                self.mmu.write(gpr[rs1].value + imm, gpr[rs2].value, Mask::Half)?
            }
            "sw" => {
                self.mmu.write(gpr[rs1].value + imm, gpr[rs2].value, Mask::Word)?
            }

            "addi" => {
                gpr[rd].value = gpr[rs1].value.wrapping_add(inst.imm);
            }

            "slti" => {
                gpr[rd].value = if (gpr[rs1].value as i32) < (inst.imm as i32) { 1 } else { 0 };
            }
            "sltiu" => {
                gpr[rd].value = if gpr[rs1].value < inst.imm { 1 } else { 0 };
            }

            "xori" => {
                gpr[rd].value = gpr[rs1].value ^ inst.imm;
            }
            "ori" => {
                gpr[rd].value = gpr[rs1].value | inst.imm;
            }
            "andi" => {
                gpr[rd].value = gpr[rs1].value & inst.imm;
            }

            "slli" => {
                gpr[rd].value = gpr[rs1].value << (inst.imm & 0x1f);
            }
            "srli" => {
                gpr[rd].value = gpr[rs1].value >> (inst.imm & 0x1f);
            }
            "srai" => {
                gpr[rd].value = (gpr[rs1].value as i32 >> (inst.imm & 0x1f)) as u32;
            }

            "add" => {
                gpr[rd].value = gpr[rs1].value.wrapping_add(gpr[rs2].value);
            }
            "sub" => {
                gpr[rd].value = gpr[rs1].value.wrapping_sub(gpr[rs2].value);
            }

            "xor" => {
                gpr[rd].value = gpr[rs1].value ^ gpr[rs2].value;
            }
            "or" => {
                gpr[rd].value = gpr[rs1].value | gpr[rs2].value;
            }
            "and" => {
                gpr[rd].value = gpr[rs1].value & gpr[rs2].value;
            }

            "slt" => {
                gpr[rd].value = if (gpr[rs1].value as i32) < (gpr[rs2].value as i32) { 1 } else { 0 };
            }
            "sltu" => {
                gpr[rd].value = if gpr[rs1].value < gpr[rs2].value { 1 } else { 0 };
            }

            "sll" => {
                gpr[rd].value = gpr[rs1].value << (gpr[rs2].value & 0x1f);
            }
            "srl" => {
                gpr[rd].value = gpr[rs1].value >> (gpr[rs2].value & 0x1f);
            }
            "sra" => {
                gpr[rd].value = (gpr[rs1].value as i32 >> (gpr[rs2].value & 0x1f)) as u32;
            }

            _ => return Ok(ExecuteResult::UnknownInst),
        }

        pc.value = npc;
        gpr[0].value = 0; // x0 is hardwired to zero

        if self.inst_trace_buffer.0 {
            self.inst_trace_buffer.1.push(inst.clone());
        }
        
        Ok(ExecuteResult::Ok)
    }

    pub fn execute(&mut self, inst: ExecuteInst) -> Result<(), simErr> {
        if self.rv32i_execute(&inst)? == ExecuteResult::Ok {
            return Ok(());
        }

        Err(simErr::InstrctionExecuteFailed { name:inst.name.to_string() })
    }
}