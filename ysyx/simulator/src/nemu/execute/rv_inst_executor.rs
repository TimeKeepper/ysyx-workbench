use crate::nemu::isa::{RISCV, RV32I, RV32M, Zicsr, Priv};

use super::super::{ExecuteInst, Simulator, sig_extend};

use msg_resp::SimErr;
use state::reg::{self, RegIdentifier, RegisterOps};
use ysyx_macro::{with_rwlock_write, with_rwlock_read};

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

        let _result = self.execute_inst(inst.clone(), &mut npc)?;

        with_rwlock_write!(self.reg, reg, {
            reg.write_pc(npc);
            reg.write_gpr(RegIdentifier::Index(0), 0)?;
        });

        if self.inst_trace_buffer.0 {
            self.inst_trace_buffer.1.push(inst);
        }

        #[cfg(all(feature = "differtest", feature = "nemu"))]
        if _result {
            self.difftest_step()?;
        } else {
            with_rwlock_read!(self.reg, reg, {
                self.differtest.set_ref_reg(&reg);
            });
        }

        Ok(())
    }

    fn execute_inst(&mut self, inst: ExecuteInst, npc: &mut u32) -> Result<bool, SimErr> {
        match inst {
            ExecuteInst { name: RISCV::RV32I(ref _op), rs1: _, rs2: _, rd: _, imm: _ } => self.rv32i_execute(&inst, npc),
            ExecuteInst { name: RISCV::RV32M(ref _op), rs1: _, rs2: _, rd: _, imm: _ } => self.rv32m_execute(&inst, npc),
            ExecuteInst { name: RISCV::Zicsr(ref _op), rs1: _, rs2: _, rd: _, imm: _ } => self.zicsr_execute(&inst, npc),
            ExecuteInst { name: RISCV::Priv(ref _op), rs1: _, rs2: _, rd: _, imm: _ }   => self.r#priv_execute(&inst, npc),
        }
    }

    fn rv32i_execute(&mut self, inst: &ExecuteInst, npc: &mut u32) -> Result<bool, SimErr> {
        let name = &inst.name;
        let rd = inst.rd as usize;
        let rs1 = inst.rs1 as usize;
        let rs2 = inst.rs2 as usize;
        let imm = inst.imm;

        let mut hazrd = false;

        match name {
            RISCV::RV32I(RV32I::Lui) => {
                with_rwlock_write!(self.reg, reg, {
                    reg.write_gpr(RegIdentifier::Index(rd), imm)?;
                });
            }
            RISCV::RV32I(RV32I::Auipc) => {
                with_rwlock_write!(self.reg, reg, {
                    let pc = reg.read_pc();
                    reg.write_gpr(RegIdentifier::Index(rd), pc.wrapping_add(imm))?;
                });
            }

            RISCV::RV32I(RV32I::Jal) => {
                with_rwlock_write!(self.reg, reg, {
                    reg.write_gpr(RegIdentifier::Index(rd), *npc)?;
                    *npc = reg.read_pc().wrapping_add(imm);
                });
            }
            RISCV::RV32I(RV32I::Jalr) => {
                with_rwlock_write!(self.reg, reg, {
                    reg.write_gpr(RegIdentifier::Index(rd), *npc)?;
                    *npc = (reg.read_gpr(RegIdentifier::Index(rs1))? + imm) & !1;
                });
            }

            RISCV::RV32I(RV32I::Beq) => {
                *npc = with_rwlock_read!(self.reg, reg, {
                    if reg.read_gpr(RegIdentifier::Index(rs1))? == reg.read_gpr(RegIdentifier::Index(rs2))? {
                        reg.read_pc().wrapping_add(imm)
                    } else {
                        *npc
                    }
                });
            }
            RISCV::RV32I(RV32I::Bne) => {
                *npc = with_rwlock_read!(self.reg, reg, {
                    if reg.read_gpr(RegIdentifier::Index(rs1))? != reg.read_gpr(RegIdentifier::Index(rs2))? {
                        reg.read_pc().wrapping_add(imm)
                    } else {
                        *npc
                    }
                });
            }
            RISCV::RV32I(RV32I::Blt) => {
                *npc = with_rwlock_read!(self.reg, reg, {
                    if (reg.read_gpr(RegIdentifier::Index(rs1))? as i32) < (reg.read_gpr(RegIdentifier::Index(rs2))? as i32) {
                        reg.read_pc().wrapping_add(imm)
                    } else {
                        *npc
                    }
                });
            }
            RISCV::RV32I(RV32I::Bge) => {
                *npc = with_rwlock_read!(self.reg, reg, {
                    if (reg.read_gpr(RegIdentifier::Index(rs1))? as i32) >= (reg.read_gpr(RegIdentifier::Index(rs2))? as i32) {
                        reg.read_pc().wrapping_add(imm)
                    } else {
                        *npc
                    }
                });
            }
            RISCV::RV32I(RV32I::Bltu) => {
                *npc = with_rwlock_read!(self.reg, reg, {
                    if reg.read_gpr(RegIdentifier::Index(rs1))? < reg.read_gpr(RegIdentifier::Index(rs2))? {
                        reg.read_pc().wrapping_add(imm)
                    } else {
                        *npc
                    }
                });
            }
            RISCV::RV32I(RV32I::Bgeu) => {
                *npc = with_rwlock_read!(self.reg, reg, {
                    if reg.read_gpr(RegIdentifier::Index(rs1))? >= reg.read_gpr(RegIdentifier::Index(rs2))? {
                        reg.read_pc().wrapping_add(imm)
                    } else {
                        *npc
                    }
                });
            }

            RISCV::RV32I(RV32I::Lb) => {
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
                        data = mem.read_device(addr, state::mmu::Mask::Byte).map_err(|e| {
                            self.resper.lock().error(format!("No matching device: {}", 
                                format!("0x{:08x}", addr)).as_str()
                            );
                            e
                        })?;
                        hazrd = true;
                    });
                    with_rwlock_write!(self.reg, reg, {
                        reg.write_gpr(RegIdentifier::Index(rd), sig_extend(data, 8))?;
                    });
                }
            }
            RISCV::RV32I(RV32I::Lh) => {
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
            RISCV::RV32I(RV32I::Lw) => {
                let addr = with_rwlock_read!(self.reg, reg, {
                    reg.read_gpr(reg::RegIdentifier::Index(rs1))?.wrapping_add(imm)
                });

                let data = with_rwlock_read!(self.mem, mem, {
                    mem.read(addr, state::mmu::Mask::Word)
                });

                if data.is_ok() {
                    with_rwlock_write!(self.reg, reg, {
                        reg.write_gpr(RegIdentifier::Index(rd), data.unwrap())?;
                    });
                } else {
                    let data: u32;
                    with_rwlock_write!(self.mem, mem, {
                        data = mem.read_device(addr, state::mmu::Mask::Word)?;
                        hazrd = true;
                    });
                    with_rwlock_write!(self.reg, reg, {
                        reg.write_gpr(RegIdentifier::Index(rd), data)?;
                    });
                }
            }
            RISCV::RV32I(RV32I::Lbu) => {
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
            RISCV::RV32I(RV32I::Lhu) => {
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

            RISCV::RV32I(RV32I::Sb) => {
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
            RISCV::RV32I(RV32I::Sh) => {
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
            RISCV::RV32I(RV32I::Sw) => {
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

            RISCV::RV32I(RV32I::Addi) => {
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1.wrapping_add(imm))?;
                });
            }

            RISCV::RV32I(RV32I::Slti) => {
                // gpr[rd] = if (gpr[rs1] as i32) < (inst.imm as i32) { 1 } else { 0 };
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    reg.write_gpr(RegIdentifier::Index(rd), if (rs1 as i32) < (imm as i32) { 1 } else { 0 })?;
                });
            }
            RISCV::RV32I(RV32I::Sltiu) => {
                // gpr[rd] = if gpr[rs1] < inst.imm { 1 } else { 0 };
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    reg.write_gpr(RegIdentifier::Index(rd), if rs1 < imm { 1 } else { 0 })?;
                });
            }

            RISCV::RV32I(RV32I::Xori) => {
                // gpr[rd] = gpr[rs1] ^ inst.imm;
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1 ^ imm)?;
                });
            }
            RISCV::RV32I(RV32I::Ori) => {
                // gpr[rd] = gpr[rs1] | inst.imm;
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1 | imm)?;
                });
            }
            RISCV::RV32I(RV32I::Andi) => {
                // gpr[rd] = gpr[rs1] & inst.imm;
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1 & imm)?;
                });
            }

            RISCV::RV32I(RV32I::Slli) => {
                // gpr[rd] = gpr[rs1] << (inst.imm & 0x1f);
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1 << (imm & 0x1f))?;
                });
            }
            RISCV::RV32I(RV32I::Srli) => {
                // gpr[rd] = gpr[rs1] >> (inst.imm & 0x1f);
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1 >> (imm & 0x1f))?;
                });
            }
            RISCV::RV32I(RV32I::Srai) => {
                // gpr[rd] = (gpr[rs1] as i32 >> (inst.imm & 0x1f)) as u32;
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    reg.write_gpr(RegIdentifier::Index(rd), (rs1 as i32 >> (imm & 0x1f)) as u32)?;
                });
            }

            RISCV::RV32I(RV32I::Add) => {
                // gpr[rd] = gpr[rs1].wrapping_add(gpr[rs2]);
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1.wrapping_add(rs2))?;
                });
            }
            RISCV::RV32I(RV32I::Sub) => {
                // gpr[rd] = gpr[rs1].wrapping_sub(gpr[rs2]);
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1.wrapping_sub(rs2))?;
                });
            }

            RISCV::RV32I(RV32I::Xor) => {
                // gpr[rd] = gpr[rs1] ^ gpr[rs2];
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1 ^ rs2)?;
                });
            }
            RISCV::RV32I(RV32I::Or) => {
                // gpr[rd] = gpr[rs1] | gpr[rs2];
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1 | rs2)?;
                });
            }
            RISCV::RV32I(RV32I::And) => {
                // gpr[rd] = gpr[rs1] & gpr[rs2];
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1 & rs2)?;
                });
            }

            RISCV::RV32I(RV32I::Slt) => {
                // gpr[rd] = if (gpr[rs1] as i32) < (gpr[rs2] as i32) { 1 } else { 0 };
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))?;
                    reg.write_gpr(RegIdentifier::Index(rd), if (rs1 as i32) < (rs2 as i32) { 1 } else { 0 })?;
                });
            }
            RISCV::RV32I(RV32I::Sltu) => {
                // gpr[rd] = if gpr[rs1] < gpr[rs2] { 1 } else { 0 };
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))?;
                    reg.write_gpr(RegIdentifier::Index(rd), if rs1 < rs2 { 1 } else { 0 })?;
                });
            }

            RISCV::RV32I(RV32I::Sll) => {
                // gpr[rd] = gpr[rs1] << (gpr[rs2] & 0x1f);
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1 << (rs2 & 0x1f))?;
                });
            }
            RISCV::RV32I(RV32I::Srl) => {
                // gpr[rd] = gpr[rs1] >> (gpr[rs2] & 0x1f);
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1 >> (rs2 & 0x1f))?;
                });
            }
            RISCV::RV32I(RV32I::Sra) => {
                // gpr[rd] = (gpr[rs1] as i32 >> (gpr[rs2] & 0x1f)) as u32;
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))?;
                    reg.write_gpr(RegIdentifier::Index(rd), (rs1 as i32 >> (rs2 & 0x1f)) as u32)?;
                });
            }

            RISCV::RV32I(RV32I::Ecall) => {
                // csr[CsrAddr::MEPC as usize] = *pc;
                // csr[CsrAddr::MCAUSE as usize] = 0x0000000b;
                // npc = csr[CsrAddr::MTVEC as usize];
                with_rwlock_write!(self.reg, reg, {
                    let pc = reg.read_pc();
                    reg.write_csr(RegIdentifier::Index(CsrAddr::MEPC as usize), pc)?;
                    reg.write_csr(RegIdentifier::Index(CsrAddr::MCAUSE as usize), 0x0000000b)?;
                    *npc = reg.read_csr(RegIdentifier::Index(CsrAddr::MTVEC as usize))?;
                });
            }
            RISCV::RV32I(RV32I::Ebreak) => {
                // return Err(SimErr::Ebreak { is_good: (gpr[10] == 0)});
                return Err(SimErr::Ebreak { is_good: with_rwlock_read!(self.reg, reg, { reg.read_gpr(RegIdentifier::Index(10))? == 0 }) });
            }

            _ => return Err(SimErr::ExecuteUnkownInst),
        }
        
        Ok(hazrd)
    }

    fn rv32m_execute(&mut self, inst: &ExecuteInst, _: &mut u32) -> Result<bool, SimErr> {
        let name = &inst.name;
        let rd = inst.rd as usize;
        let rs1 = inst.rs1 as usize;
        let rs2 = inst.rs2 as usize;

        match name {
            RISCV::RV32M(RV32M::Mul) => {
                // gpr[rd] = gpr[rs1].wrapping_mul(gpr[rs2]);
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))?;
                    reg.write_gpr(RegIdentifier::Index(rd), rs1.wrapping_mul(rs2))?;
                });
            }

            RISCV::RV32M(RV32M::Mulh) => {
                // let result = (gpr[rs1] as i64).wrapping_mul(gpr[rs2] as i64);
                // gpr[rd] = (result >> 32) as u32;
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))? as i64;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))? as i64;
                    reg.write_gpr(RegIdentifier::Index(rd), (rs1.wrapping_mul(rs2) >> 32) as u32)?;
                });
            }
            RISCV::RV32M(RV32M::Mulhsu) => {
                // let result = (gpr[rs1] as i64).wrapping_mul((gpr[rs2] as u64).try_into().unwrap());
                // gpr[rd] = (result >> 32) as u32;
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))? as i64;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))? as u64;
                    reg.write_gpr(RegIdentifier::Index(rd), (rs1.wrapping_mul(rs2.try_into().unwrap()) >> 32) as u32)?;
                });
            }
            RISCV::RV32M(RV32M::Mulhu) => {
                // let result = (gpr[rs1] as u64).wrapping_mul(gpr[rs2] as u64);
                // gpr[rd] = (result >> 32) as u32;
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))? as u64;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))? as u64;
                    reg.write_gpr(RegIdentifier::Index(rd), (rs1.wrapping_mul(rs2) >> 32) as u32)?;
                });
            }

            RISCV::RV32M(RV32M::Div) => {
                // if gpr[rs2] == 0 {
                //     gpr[rd] = 0xffffffff;
                // } else {
                //     gpr[rd] = (gpr[rs1] as i32).wrapping_div(gpr[rs2] as i32) as u32;
                // }
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))? as i32;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))? as i32;
                    reg.write_gpr(RegIdentifier::Index(rd), if rs2 == 0 { 0xffffffff } else { (rs1.wrapping_div(rs2)) as u32 })?;
                });
            }
            RISCV::RV32M(RV32M::Divu) => {
                // if gpr[rs2] == 0 {
                //     gpr[rd] = 0xffffffff;
                // } else {
                //     gpr[rd] = gpr[rs1].wrapping_div(gpr[rs2]);
                // }
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))?;
                    reg.write_gpr(RegIdentifier::Index(rd), if rs2 == 0 { 0xffffffff } else { rs1.wrapping_div(rs2) })?;
                });
            }

            RISCV::RV32M(RV32M::Rem) => {
                // if gpr[rs2] == 0 {
                //     gpr[rd] = gpr[rs1];
                // } else {
                //     gpr[rd] = (gpr[rs1] as i32).wrapping_rem(gpr[rs2] as i32) as u32;
                // }
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))? as i32;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))? as i32;
                    reg.write_gpr(RegIdentifier::Index(rd), if rs2 == 0 { rs1 as u32 } else { (rs1.wrapping_rem(rs2)) as u32 })?;
                });
            }
            RISCV::RV32M(RV32M::Remu) => {
                // if gpr[rs2] == 0 {
                //     gpr[rd] = gpr[rs1];
                // } else {
                //     gpr[rd] = gpr[rs1].wrapping_rem(gpr[rs2]);
                // }
                with_rwlock_write!(self.reg, reg, {
                    let rs1 = reg.read_gpr(reg::RegIdentifier::Index(rs1))?;
                    let rs2 = reg.read_gpr(reg::RegIdentifier::Index(rs2))?;
                    reg.write_gpr(RegIdentifier::Index(rd), if rs2 == 0 { rs1 } else { rs1.wrapping_rem(rs2) })?;
                });
            }

            _ => return Err(SimErr::ExecuteUnkownInst),
        }

        Ok(false)
    }

    fn zicsr_execute(&mut self, inst: &ExecuteInst, _: &mut u32) -> Result<bool, SimErr> {
        let name = &inst.name;
        let rd = inst.rd as usize;
        let rs1 = inst.rs1 as usize;

        match name {
            RISCV::Zicsr(Zicsr::Csrrw) => {
                // let csr_t = csr[inst.imm as usize];

                // csr[inst.imm as usize] = gpr[rs1];
                // gpr[rd] = csr_t;
                with_rwlock_write!(self.reg, reg, {
                    let csr_t = reg.read_csr(RegIdentifier::Index(inst.imm as usize))?;
                    let rs1 = reg.read_gpr(RegIdentifier::Index(rs1))?;
                    reg.write_csr(RegIdentifier::Index(inst.imm as usize), rs1)?;
                    reg.write_gpr(RegIdentifier::Index(rd), csr_t)?;
                });
            }
            RISCV::Zicsr(Zicsr::Csrrs) => {
                // let csr_t = csr[inst.imm as usize];

                // csr[inst.imm as usize] |= gpr[rs1];
                // gpr[rd] = csr_t;
                with_rwlock_write!(self.reg, reg, {
                    let csr_t = reg.read_csr(RegIdentifier::Index(inst.imm as usize))?;
                    let rs1 = reg.read_gpr(RegIdentifier::Index(rs1))?;
                    reg.write_csr(RegIdentifier::Index(inst.imm as usize), csr_t | rs1)?;
                    reg.write_gpr(RegIdentifier::Index(rd), csr_t)?;
                });
            }

            _ => return Err(SimErr::ExecuteUnkownInst),
        }

        Ok(false)
    }

    fn r#priv_execute(&mut self, inst: &ExecuteInst, npc: &mut u32) -> Result<bool, SimErr> {
        let name = &inst.name;

        match name {
            RISCV::Priv(Priv::Mret) => {
                // npc = csr[CsrAddr::MEPC as usize];
                with_rwlock_write!(self.reg, reg, {
                    *npc = reg.read_csr(RegIdentifier::Index(CsrAddr::MEPC as usize))?;
                });
            }

            _ => return Err(SimErr::ExecuteUnkownInst),
        }

        Ok(false)
    }
}