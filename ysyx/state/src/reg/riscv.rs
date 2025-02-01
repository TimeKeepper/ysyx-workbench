use std::io::Write;

#[cfg(feature = "riscv32")]
use bimap::BiHashMap;
use msg_resp::{ResultMessage, SimErr, SimOk};
use owo_colors::OwoColorize;
use tabwriter::TabWriter;

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

use super::{RegIdentifier, RegisterOps, RegType};

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

    fn print_reg(&self, specify: Option<String>, r#type: RegType) -> ResultMessage {
        let map = match r#type {
            RegType::GPR => &self.gp_map,
            RegType::CSR => &self.cs_map,
        };

        let read_reg: Box<dyn Fn(RegIdentifier) -> Result<u32, SimErr>> = match r#type {
            RegType::GPR => Box::new(|id| self.read_gpr(id)),
            RegType::CSR => Box::new(|id| self.read_csr(id)),
        };

        let mut tw = TabWriter::new(vec![]);

        if specify.is_none() {
            for (name, index) in map.iter() {
                writeln!(&mut tw, "{}\t0x{:08x}", 
                    name.purple(), 
                    read_reg(RegIdentifier::Index(*index)).unwrap().red())
                .unwrap();
            }
            tw.flush().unwrap();

            print!("{}", String::from_utf8(tw.into_inner().unwrap()).unwrap());
            return Ok(SimOk::Nothing);
        }

        let specify_str = specify.unwrap();
        let index = specify_str.parse::<usize>();

        if index.is_ok() {
            let index = index.unwrap();

            writeln!(&mut tw, "{}\t0x{:08x}", 
                map.get_by_right(&index).unwrap().purple(),
                read_reg(RegIdentifier::Index(index))?.red()
            ).unwrap();
            tw.flush().unwrap();

            print!("{}", String::from_utf8(tw.into_inner().unwrap()).unwrap());
            return Ok(SimOk::Nothing);
        }

        writeln!(&mut tw, "{}\t0x{:08x}", 
            specify_str.purple(), 
            read_reg(RegIdentifier::Name(&specify_str))?.red()
        ).unwrap();
        tw.flush().unwrap();
        print!("{}", String::from_utf8(tw.into_inner().unwrap()).unwrap());
        
        return Ok(SimOk::Nothing);
    }
}
