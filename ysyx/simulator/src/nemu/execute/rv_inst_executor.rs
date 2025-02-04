use crate::npc;

use super::super::{ExecuteInst, Simulator, sig_extend};

use msg_resp::SimErr;
use state::reg::{self, RegIdentifier, RegisterOps};
use ysyx_macro::{with_mutex_lock, with_rwlock_write, with_rwlock_read};


#[derive(Debug, PartialEq, Clone)]
enum ExecuteResult {
    UnknownInst,
    Ok { is_state_hazard: bool },
}

#[derive(Debug, PartialEq, Clone)]
#[allow(dead_code)]
enum CsrAddr {
    MSTATUS = 0x300,
    MTVEC = 0x305,
    MSCRATCH = 0x340,
    MEPC = 0x341,
    MCAUSE = 0x342,
    MVENDORID = 0xf11,
    MARCHID = 0xf12,
}

impl Simulator {
    pub fn execute(&mut self, inst: ExecuteInst) -> Result<(), SimErr> {
        let mut npc = with_rwlock_read!(self.reg, reg, {
            reg.read_pc().wrapping_add(4)
        });

        let hazard = self.execute_inst(inst.clone(), &mut npc)?;

        #[cfg(feature = "differtest")]
        if !hazard {
            self.differtest.ref_difftest_exec(1);
        }

        with_rwlock_write!(self.reg, reg, {
            reg.write_pc(npc);
            reg.write_gpr(RegIdentifier::Index(0), 0)?;
        });

        if self.inst_trace_buffer.0 {
            self.inst_trace_buffer.1.push(inst);
        }

        Ok(())
    }

    fn execute_inst(&mut self, inst: ExecuteInst, npc: &mut u32) -> Result<bool, SimErr> {
        match self.rv32i_execute(&inst, npc)? {
            ExecuteResult::Ok { is_state_hazard } => return Ok(is_state_hazard),
            ExecuteResult::UnknownInst => (),
        }

        // if self.rv32m_execute(&inst)? == ExecuteResult::Ok {
        //     return Ok(());
        // }

        // if self.zicsr_execute(&inst)? == ExecuteResult::Ok {
        //     return Ok(());
        // }

        // if self.r#priv_execute(&inst)? == ExecuteResult::Ok {
        //     return Ok(());
        // }

        // Err(SimErr::InstrctionExecuteFailed { name:inst.name.to_string() })
        Err(SimErr::InstrctionExecuteFailed)
    }

    fn rv32i_execute(&mut self, inst: &ExecuteInst, npc: &mut u32) -> Result<ExecuteResult, SimErr> {
        let name: &str = &inst.name;
        let rd = inst.rd as usize;
        let rs1 = inst.rs1 as usize;
        let rs2 = inst.rs2 as usize;
        let imm = inst.imm;

        let mut hazrd = false;

        match name {
            "lui" => {
                with_rwlock_write!(self.reg, reg, {
                    reg.write_gpr(RegIdentifier::Index(rd), imm)?;
                });
            }
            "auipc" => {
                with_rwlock_write!(self.reg, reg, {
                    let pc = reg.read_pc();
                    reg.write_gpr(RegIdentifier::Index(rd), pc.wrapping_add(imm))?;
                });
            }

            "jal" => {
                with_rwlock_write!(self.reg, reg, {
                    reg.write_gpr(RegIdentifier::Index(rd), *npc)?;
                    *npc = npc.wrapping_add(imm);
                });
            }
            "jalr" => {
                with_rwlock_write!(self.reg, reg, {
                    reg.write_gpr(RegIdentifier::Index(rd), *npc)?;
                    *npc = (reg.read_gpr(RegIdentifier::Index(rs1))? + imm) & !1;
                });
            }

            "beq" => {
                *npc = with_rwlock_read!(self.reg, reg, {
                    if reg.read_gpr(RegIdentifier::Index(rs1))? == reg.read_gpr(RegIdentifier::Index(rs2))? {
                        reg.read_pc().wrapping_add(imm)
                    } else {
                        *npc
                    }
                });
            }
            "bne" => {
                *npc = with_rwlock_read!(self.reg, reg, {
                    if reg.read_gpr(RegIdentifier::Index(rs1))? != reg.read_gpr(RegIdentifier::Index(rs2))? {
                        reg.read_pc().wrapping_add(imm)
                    } else {
                        *npc
                    }
                });
            }
            "blt" => {
                *npc = with_rwlock_read!(self.reg, reg, {
                    if (reg.read_gpr(RegIdentifier::Index(rs1))? as i32) < (reg.read_gpr(RegIdentifier::Index(rs2))? as i32) {
                        reg.read_pc().wrapping_add(imm)
                    } else {
                        *npc
                    }
                });
            }
            "bge" => {
                *npc = with_rwlock_read!(self.reg, reg, {
                    if (reg.read_gpr(RegIdentifier::Index(rs1))? as i32) >= (reg.read_gpr(RegIdentifier::Index(rs2))? as i32) {
                        reg.read_pc().wrapping_add(imm)
                    } else {
                        *npc
                    }
                });
            }
            "bltu" => {
                *npc = with_rwlock_read!(self.reg, reg, {
                    if reg.read_gpr(RegIdentifier::Index(rs1))? < reg.read_gpr(RegIdentifier::Index(rs2))? {
                        reg.read_pc().wrapping_add(imm)
                    } else {
                        *npc
                    }
                });
            }
            "bgeu" => {
                *npc = with_rwlock_read!(self.reg, reg, {
                    if reg.read_gpr(RegIdentifier::Index(rs1))? >= reg.read_gpr(RegIdentifier::Index(rs2))? {
                        reg.read_pc().wrapping_add(imm)
                    } else {
                        *npc
                    }
                });
            }

            "lb" => {
                let addr = with_rwlock_read!(self.reg, reg, {
                    reg.read_gpr(reg::RegIdentifier::Index(rs1))?.wrapping_add(imm)
                });

                let data = with_rwlock_read!(self.mem, mem, {
                    mem.read(addr, state::mmu::Mask::Byte)
                });

                if data.is_ok() {
                    with_rwlock_write!(self.reg, reg, {
                        reg.write_gpr(RegIdentifier::Index(rd), sig_extend(data.unwrap(), 8))?;
                    });
                } else {
                    let data: u32;
                    with_rwlock_write!(self.mem, mem, {
                        data = mem.read_device(addr, state::mmu::Mask::Byte)?;
                        hazrd = true;
                    });
                    with_rwlock_write!(self.reg, reg, {
                        reg.write_gpr(RegIdentifier::Index(rd), sig_extend(data, 8))?;
                    });
                }
            }
            "lh" => {
                let addr = with_rwlock_read!(self.reg, reg, {
                    reg.read_gpr(reg::RegIdentifier::Index(rs1))?.wrapping_add(imm)
                });

                let data = with_rwlock_read!(self.mem, mem, {
                    mem.read(addr, state::mmu::Mask::Half)
                });

                if data.is_ok() {
                    with_rwlock_write!(self.reg, reg, {
                        reg.write_gpr(RegIdentifier::Index(rd), sig_extend(data.unwrap(), 16))?;
                    });
                } else {
                    let data: u32;
                    with_rwlock_write!(self.mem, mem, {
                        data = mem.read_device(addr, state::mmu::Mask::Half)?;
                        hazrd = true;
                    });
                    with_rwlock_write!(self.reg, reg, {
                        reg.write_gpr(RegIdentifier::Index(rd), sig_extend(data, 16))?;
                    });
                }
            }
            "lw" => {
                let addr = with_rwlock_read!(self.reg, reg, {
                    reg.read_gpr(reg::RegIdentifier::Index(rs1))?.wrapping_add(imm)
                });

                let data = with_rwlock_read!(self.mem, mem, {
                    mem.read(addr, state::mmu::Mask::Half)
                });

                if data.is_ok() {
                    with_rwlock_write!(self.reg, reg, {
                        reg.write_gpr(RegIdentifier::Index(rd), data.unwrap())?;
                    });
                } else {
                    let data: u32;
                    with_rwlock_write!(self.mem, mem, {
                        data = mem.read_device(addr, state::mmu::Mask::Half)?;
                        hazrd = true;
                    });
                    with_rwlock_write!(self.reg, reg, {
                        reg.write_gpr(RegIdentifier::Index(rd), data)?;
                    });
                }
            }
            "lbu" => {
                let addr = with_rwlock_read!(self.reg, reg, {
                    reg.read_gpr(reg::RegIdentifier::Index(rs1))?.wrapping_add(imm)
                });

                let data = with_rwlock_read!(self.mem, mem, {
                    mem.read(addr, state::mmu::Mask::Byte)
                });

                if data.is_ok() {
                    with_rwlock_write!(self.reg, reg, {
                        reg.write_gpr(RegIdentifier::Index(rd), data.unwrap())?;
                    });
                } else {
                    let data: u32;
                    with_rwlock_write!(self.mem, mem, {
                        data = mem.read_device(addr, state::mmu::Mask::Byte)?;
                        hazrd = true;
                    });
                    with_rwlock_write!(self.reg, reg, {
                        reg.write_gpr(RegIdentifier::Index(rd), data)?;
                    });
                }
            }
            "lhu" => {
                let addr = with_rwlock_read!(self.reg, reg, {
                    reg.read_gpr(reg::RegIdentifier::Index(rs1))?.wrapping_add(imm)
                });

                let data = with_rwlock_read!(self.mem, mem, {
                    mem.read(addr, state::mmu::Mask::Half)
                });

                if data.is_ok() {
                    with_rwlock_write!(self.reg, reg, {
                        reg.write_gpr(RegIdentifier::Index(rd), data.unwrap())?;
                    });
                } else {
                    let data: u32;
                    with_rwlock_write!(self.mem, mem, {
                        data = mem.read_device(addr, state::mmu::Mask::Half)?;
                        hazrd = true;
                    });
                    with_rwlock_write!(self.reg, reg, {
                        reg.write_gpr(RegIdentifier::Index(rd), data)?;
                    });
                }
            }

            "sb" => {
                let (addr, data) = with_rwlock_read!(self.reg, reg, {
                    (reg.read_gpr(reg::RegIdentifier::Index(rs1))?.wrapping_add(imm), reg.read_gpr(reg::RegIdentifier::Index(rs2))?)
                });
                with_rwlock_write!(self.mem, mem, {
                    let result =  mem.write(addr, data, state::mmu::Mask::Byte);
                    if result.is_err() {
                        mem.write_device(addr, data, state::mmu::Mask::Byte)?;
                        hazrd = true;
                    }
                });
            }
            "sh" => {
                let (addr, data) = with_rwlock_read!(self.reg, reg, {
                    (reg.read_gpr(reg::RegIdentifier::Index(rs1))?.wrapping_add(imm), reg.read_gpr(reg::RegIdentifier::Index(rs2))?)
                });
                with_rwlock_write!(self.mem, mem, {
                    let result =  mem.write(addr, data, state::mmu::Mask::Half);
                    if result.is_err() {
                        mem.write_device(addr, data, state::mmu::Mask::Half)?;
                        hazrd = true;
                    }
                });
            }
            "sw" => {
                let (addr, data) = with_rwlock_read!(self.reg, reg, {
                    (reg.read_gpr(reg::RegIdentifier::Index(rs1))?.wrapping_add(imm), reg.read_gpr(reg::RegIdentifier::Index(rs2))?)
                });
                with_rwlock_write!(self.mem, mem, {
                    let result =  mem.write(addr, data, state::mmu::Mask::Word);
                    if result.is_err() {
                        mem.write_device(addr, data, state::mmu::Mask::Word)?;
                        hazrd = true;
                    }
                });
            }

            "addi" => {
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1.wrapping_add(imm))?;
                });
            }

            "slti" => {
                // gpr[rd] = if (gpr[rs1] as i32) < (inst.imm as i32) { 1 } else { 0 };
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    reg.write_gpr(RegIdentifier::Index(rd), if (rs1 as i32) < (imm as i32) { 1 } else { 0 })?;
                });
            }
            // "sltiu" => {
            //     gpr[rd] = if gpr[rs1] < inst.imm { 1 } else { 0 };
            // }

            // "xori" => {
            //     gpr[rd] = gpr[rs1] ^ inst.imm;
            // }
            // "ori" => {
            //     gpr[rd] = gpr[rs1] | inst.imm;
            // }
            // "andi" => {
            //     gpr[rd] = gpr[rs1] & inst.imm;
            // }

            // "slli" => {
            //     gpr[rd] = gpr[rs1] << (inst.imm & 0x1f);
            // }
            // "srli" => {
            //     gpr[rd] = gpr[rs1] >> (inst.imm & 0x1f);
            // }
            // "srai" => {
            //     gpr[rd] = (gpr[rs1] as i32 >> (inst.imm & 0x1f)) as u32;
            // }

            // "add" => {
            //     gpr[rd] = gpr[rs1].wrapping_add(gpr[rs2]);
            // }
            // "sub" => {
            //     gpr[rd] = gpr[rs1].wrapping_sub(gpr[rs2]);
            // }

            // "xor" => {
            //     gpr[rd] = gpr[rs1] ^ gpr[rs2];
            // }
            // "or" => {
            //     gpr[rd] = gpr[rs1] | gpr[rs2];
            // }
            // "and" => {
            //     gpr[rd] = gpr[rs1] & gpr[rs2];
            // }

            // "slt" => {
            //     gpr[rd] = if (gpr[rs1] as i32) < (gpr[rs2] as i32) { 1 } else { 0 };
            // }
            // "sltu" => {
            //     gpr[rd] = if gpr[rs1] < gpr[rs2] { 1 } else { 0 };
            // }

            // "sll" => {
            //     gpr[rd] = gpr[rs1] << (gpr[rs2] & 0x1f);
            // }
            // "srl" => {
            //     gpr[rd] = gpr[rs1] >> (gpr[rs2] & 0x1f);
            // }
            // "sra" => {
            //     gpr[rd] = (gpr[rs1] as i32 >> (gpr[rs2] & 0x1f)) as u32;
            // }

            // "ecall" => {
            //     csr[CsrAddr::MEPC as usize] = *pc;
            //     csr[CsrAddr::MCAUSE as usize] = 0x0000000b;
            //     npc = csr[CsrAddr::MTVEC as usize];
            // }
            // "ebreak" => {
            //     return Err(SimErr::Ebreak { is_good: (gpr[10] == 0)});
            // }

            _ => return Ok(ExecuteResult::UnknownInst),
        }
        
        Ok(ExecuteResult::Ok { is_state_hazard: hazrd })
    }

//     fn rv32m_execute(&mut self, inst: &ExecuteInst) -> Result<ExecuteResult, SimErr> {
//         let name: &str = &inst.name;
//         let rd = inst.rd as usize;
//         let rs1 = inst.rs1 as usize;
//         let rs2 = inst.rs2 as usize;
        
//         let npc = self.cpu_state.pc + 4;
//         let gpr = &mut self.cpu_state.gpr;
//         let pc= &mut self.cpu_state.pc;

//         match name {
//             "mul" => {
//                 gpr[rd] = gpr[rs1].wrapping_mul(gpr[rs2]);
//             }

//             "mulh" => {
//                 let result = (gpr[rs1] as i64).wrapping_mul(gpr[rs2] as i64);
//                 gpr[rd] = (result >> 32) as u32;
//             }
//             "mulhsu" => {
//                 let result = (gpr[rs1] as i64).wrapping_mul((gpr[rs2] as u64).try_into().unwrap());
//                 gpr[rd] = (result >> 32) as u32;
//             }
//             "mulhu" => {
//                 let result = (gpr[rs1] as u64).wrapping_mul(gpr[rs2] as u64);
//                 gpr[rd] = (result >> 32) as u32;
//             }

//             "div" => {
//                 if gpr[rs2] == 0 {
//                     gpr[rd] = 0xffffffff;
//                 } else {
//                     gpr[rd] = (gpr[rs1] as i32).wrapping_div(gpr[rs2] as i32) as u32;
//                 }
//             }
//             "divu" => {
//                 if gpr[rs2] == 0 {
//                     gpr[rd] = 0xffffffff;
//                 } else {
//                     gpr[rd] = gpr[rs1].wrapping_div(gpr[rs2]);
//                 }
//             }

//             "rem" => {
//                 if gpr[rs2] == 0 {
//                     gpr[rd] = gpr[rs1];
//                 } else {
//                     gpr[rd] = (gpr[rs1] as i32).wrapping_rem(gpr[rs2] as i32) as u32;
//                 }
//             }
//             "remu" => {
//                 if gpr[rs2] == 0 {
//                     gpr[rd] = gpr[rs1];
//                 } else {
//                     gpr[rd] = gpr[rs1].wrapping_rem(gpr[rs2]);
//                 }
//             }

//             _ => return Ok(ExecuteResult::UnknownInst),
//         }

//         *pc = npc;
//         gpr[0] = 0; // x0 is hardwired to zero

//         if self.inst_trace_buffer.0 {
//             self.inst_trace_buffer.1.push(inst.clone());
//         }

//         Ok(ExecuteResult::Ok)
//     }

//     fn zicsr_execute(&mut self, inst: &ExecuteInst) -> Result<ExecuteResult, SimErr> {
//         let name: &str = &inst.name;
//         let rd = inst.rd as usize;
//         let rs1 = inst.rs1 as usize;
        
//         let npc = self.cpu_state.pc + 4;
//         let gpr = &mut self.cpu_state.gpr;
//         let pc= &mut self.cpu_state.pc;
//         let csr = &mut self.cpu_state.csr;

//         match name {
//             "csrrw" => {
//                 let csr_t = csr[inst.imm as usize];

//                 csr[inst.imm as usize] = gpr[rs1];
//                 gpr[rd] = csr_t;
//             }
//             "csrrs" => {
//                 let csr_t = csr[inst.imm as usize];

//                 csr[inst.imm as usize] |= gpr[rs1];
//                 gpr[rd] = csr_t;
//             }

//             _ => return Ok(ExecuteResult::UnknownInst),
//         }

//         *pc = npc;
//         gpr[0] = 0; // x0 is hardwired to zero

//         if self.inst_trace_buffer.0 {
//             self.inst_trace_buffer.1.push(inst.clone());
//         }

//         Ok(ExecuteResult::Ok)
//     }

//     fn r#priv_execute(&mut self, inst: &ExecuteInst) -> Result<ExecuteResult, SimErr> {
//         let name: &str = &inst.name;
        
//         let npc: u32;
//         let gpr = &mut self.cpu_state.gpr;
//         let pc= &mut self.cpu_state.pc;
//         let csr = &mut self.cpu_state.csr;

//         match name {
//             "mret" => {
//                 npc = csr[CsrAddr::MEPC as usize];
//             }

//             _ => return Ok(ExecuteResult::UnknownInst),
//         }

//         *pc = npc;
//         gpr[0] = 0; // x0 is hardwired to zero

//         if self.inst_trace_buffer.0 {
//             self.inst_trace_buffer.1.push(inst.clone());
//         }

//         Ok(ExecuteResult::Ok)
//     }
}