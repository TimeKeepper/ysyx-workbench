#[cfg(feature = "riscv32")]
const RV32GPR_NAME: [&'static str; 32] = [
    "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2", "s0", "s1", "a0", "a1", "a2", "a3", "a4",
    "a5", "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11", "t3", "t4",
    "t5", "t6",
];

#[cfg(feature = "riscv32")]
pub struct RegisterBank {
    pub gp: [u32; 32],
    pub pc: u32,
    pub cs: [u32; 4096],
}

#[cfg(feature = "riscv32")]
impl RegisterBank {
    pub fn new() -> Self {
        RegisterBank {
            gp: [0; 32],
            pc: 0,
            cs: [0; 4096],
        }
    }

    pub fn name2index(&self, name: &str) -> Option<u32> {
        for (i, n) in RV32GPR_NAME.iter().enumerate() {
            if *n == name {
                return Some(i as u32);
            }
        }
        None
    }

    pub fn index2name(&self, index: u32) -> Option<&'static str> {
        if (0..32).contains(&index) {
            return Some(RV32GPR_NAME[index as usize]);
        }
        None
    }
}

use super::{RegIdentifier, RegisterOps};

#[cfg(feature = "riscv32")]
impl RegisterOps for RegisterBank {
    fn read_gpr(&self, reg: RegIdentifier) -> Result<u32, ()> {
        match reg {
            RegIdentifier::Index(index) => {
                if (0..32).contains(&index) {
                    Ok(self.gp[index])
                } else {
                    Err(())
                }
            }
            RegIdentifier::Name(name) => {
                if let Some(index) = self.name2index(name) {
                    Ok(self.gp[index as usize])
                } else {
                    Err(())
                }
            }
        }
    }

    fn write_gpr(&mut self, reg: RegIdentifier, value: u32) -> Result<(), ()> {
        match reg {
            RegIdentifier::Index(index) => {
                if (0..32).contains(&index) {
                    self.gp[index] = value;
                    Ok(())
                } else {
                    Err(())
                }
            }
            RegIdentifier::Name(name) => {
                if let Some(index) = self.name2index(name) {
                    self.gp[index as usize] = value;
                    Ok(())
                } else {
                    Err(())
                }
            }
        }
    }

    fn read_pc(&self) -> u32 {
        self.pc
    }

    fn write_pc(&mut self, value: u32) {
        self.pc = value;
    }
}
