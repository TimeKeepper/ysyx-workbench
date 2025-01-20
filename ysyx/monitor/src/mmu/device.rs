use crate::mmu::MMT;
use std::ops::Range;

use super::{Mask, MatchMsg};

pub struct Reg {
    addr: u32,
    value: u32,
}

impl Reg {
    pub fn read(&self, mask: Mask) -> u32 {
        match mask {
            Mask::Byte => {
                self.value & 0xff
            }
            Mask::Half => {
                self.value & 0xffff
            }
            Mask::Word => {
                self.value
            }
            Mask::None => {
                self.value
            }
        }
    }

    pub fn write(&mut self, data: u32, mask: Mask) {
        match mask {
            Mask::Byte => {
                self.value = (self.value & 0xffffff00) | (data & 0xff);
            }
            Mask::Half => {
                self.value = (self.value & 0xffff0000) | (data & 0xffff);
            }
            Mask::Word => {
                self.value = data;
            }
            Mask::None => {
                self.value = data;
            }
        }
    }
}

pub struct Device {
    pub name: String,
    pub range: Range<u32>,
    regs: Vec<Reg>,
}

impl Device {
    pub fn new(name: &str, range: Range<u32>) -> Self {
        Device {
            name: name.to_string(),
            range,
            regs: Vec::new(),
        }
    }

    pub fn find_reg(&mut self, addr: u32) -> Option<&mut Reg> {
        for r in self.regs.iter_mut() {
            if r.addr == addr {
                return Some(r);
            }
        }
        None
    }
}

impl MMT for Device {
    fn match_memory(&mut self, msg: MatchMsg) -> bool {
        match msg {
            MatchMsg::ADDR { addr } => {
                if addr >= self.range.start && addr < self.range.end {
                    true
                } else {
                    false
                }
            }
            MatchMsg::NAME { name } => {
                if self.name == name {
                    true
                } else {
                    false
                }
            }
        }
    }

    fn read(&mut self, addr: u32, mask: super::Mask) -> u32 {
        let reg = self.find_reg(addr);
        if let Some(r) = reg {
            r.read(mask)
        } else {
            0
        }
    }

    fn write(&mut self, addr: u32, data: u32, mask: super::Mask) {
        let reg = self.find_reg(addr);
        if let Some(r) = reg {
            r.write(data, mask);
        }
    }

    fn load(&mut self, data: &[u8]) -> Result<(), crate::SimulatorError> {
        let _ = data;
        panic!("Device should not load data");
    }

    fn get_memory(&mut self) -> &mut [u8] {
        panic!("Device should not get memory");
    }
}
