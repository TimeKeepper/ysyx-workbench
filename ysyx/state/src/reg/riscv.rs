#[cfg(feature = "riscv32")]
use bimap::BiHashMap;
use msg_resp::{ResultMessage, SimErr, SimOk};
use owo_colors::OwoColorize;

#[cfg(feature = "riscv32")]
pub struct RegisterBank {
    pub gp: [u32; 32],
    gp_map: BiHashMap<&'static str, usize>,
    pub pc: u32,
    pub cs: [u32; 4096],
    cs_map: BiHashMap<&'static str, usize>,
}

#[cfg(feature = "riscv32")]
impl RegisterBank {
    pub fn new() -> Self {
        let rvgpr_name: [&str; 32] = [
            "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2", "s0", "s1", "a0", "a1", "a2", "a3", "a4",
            "a5", "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11", "t3", "t4",
            "t5", "t6",
        ];

        let mut gp_map: BiHashMap<_, usize> = BiHashMap::new();
        for (i, n) in rvgpr_name.iter().enumerate() {
            gp_map.insert(*n, i);
        }

        let mut cs_map: BiHashMap<_, usize> = BiHashMap::new();
        cs_map.insert("mstatus",    0x300);
        cs_map.insert("mtvec",      0x305);
        cs_map.insert("mscratch",   0x340);
        cs_map.insert("mepc",       0x341);
        cs_map.insert("mcause",     0x342);
        cs_map.insert("mvendorid",  0xf11);
        cs_map.insert("marchid",    0xf12);
        
        RegisterBank {
            gp: [0; 32],
            gp_map,
            pc: 0,
            cs: [0; 4096],
            cs_map,
        }
    }
}

use super::{RegIdentifier, RegisterOps};

#[cfg(feature = "riscv32")]
impl RegisterOps for RegisterBank {
    fn read_gpr(&self, reg: RegIdentifier) -> Result<u32, SimErr> {
        match reg {
            RegIdentifier::Index(index) => {
                if (0..32).contains(&index) {
                    Ok(self.gp[index])
                } else {
                    Err(SimErr::InvalidRegIndentifier)
                }
            }
            RegIdentifier::Name(name) => {
                if let Some(index) = self.gp_map.get_by_left(name).copied() {
                    Ok(self.gp[index])
                } else {
                    Err(SimErr::InvalidRegIndentifier)
                }
            }
        }
    }

    fn write_gpr(&mut self, reg: RegIdentifier, value: u32) -> ResultMessage {
        match reg {
            RegIdentifier::Index(index) => {
                if (0..32).contains(&index) {
                    self.gp[index] = value;
                    Ok(SimOk::Nothing)
                } else {
                    Err(SimErr::InvalidRegIndentifier)
                }
            }
            RegIdentifier::Name(name) => {
                if let Some(index) = self.gp_map.get_by_left(name).copied() {
                    self.gp[index] = value;
                    Ok(SimOk::Nothing)
                } else {
                    Err(SimErr::InvalidRegIndentifier)
                }
            }
        }
    }

    fn print_gpr(&self, specify: Option<String>) -> ResultMessage {
        if specify.is_none() {
            for (name, index) in self.gp_map.iter() {
                println!("{:<}: \t0x{:<08x}", name.purple(), self.gp[*index].red());
            }
            return Ok(SimOk::Nothing);
        }
        println!("{:<}: \t0x{:<08x}", 
            specify.clone().unwrap().purple(), 
            self.read_gpr(RegIdentifier::Name(&specify.unwrap()))?.red()
        );
        return Ok(SimOk::Nothing);
    }

    fn read_pc(&self) -> u32 {
        self.pc
    }

    fn write_pc(&mut self, value: u32) {
        self.pc = value;
    }
    
    fn read_csr(&self, reg: RegIdentifier) -> Result<u32, SimErr> {
        match reg {
            RegIdentifier::Index(index) => {
                if (0..4096).contains(&index) {
                    Ok(self.cs[index])
                } else {
                    Err(SimErr::InvalidRegIndentifier)
                }
            }
            RegIdentifier::Name(name) => {
                if let Some(index) = self.cs_map.get_by_left(name).copied() {
                    Ok(self.cs[index])
                } else {
                    Err(SimErr::InvalidRegIndentifier)
                }
            }
        }
    }

    fn write_csr(&mut self, reg: RegIdentifier, value: u32) -> ResultMessage {
        match reg {
            RegIdentifier::Index(index) => {
                if (0..4096).contains(&index) {
                    self.cs[index] = value;
                    Ok(SimOk::Nothing)
                } else {
                    Err(SimErr::InvalidRegIndentifier)
                }
            }
            RegIdentifier::Name(name) => {
                if let Some(index) = self.cs_map.get_by_left(name).copied() {
                    self.cs[index] = value;
                    Ok(SimOk::Nothing)
                } else {
                    Err(SimErr::InvalidRegIndentifier)
                }
            }
        }
    }

    fn print_csr(&self, specify: Option<String>) -> ResultMessage {
        if specify.is_none() {
            for (name, index) in self.cs_map.iter() {
                println!("{:<}: 0x{:<08x}", name.purple(), self.cs[*index].red());
            }
            return Ok(SimOk::Nothing);
        }
        println!("{:<}: 0x{:<08x}", 
            specify.clone().unwrap().purple(), 
            self.read_csr(RegIdentifier::Name(&specify.unwrap()))?.red()
        );
        return Ok(SimOk::Nothing);
    }
}
