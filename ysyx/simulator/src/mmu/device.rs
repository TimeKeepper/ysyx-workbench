use std::{collections::HashMap, ops::Range};

use super::{Mask, MatchMsg};

pub enum AttchDirection {
    Read,
    Write,
}

pub struct Device {
    pub name: String,
    pub range: Range<u32>,

    callback: Option<Box<dyn FnMut(AttchDirection, u32, Option<u32>, Mask) -> u32>>
}

impl Device {
    pub fn new(name: &str, range: Range<u32>, callback: Option<Box<dyn FnMut(AttchDirection, u32, Option<u32>, Mask) -> u32>>) -> Self {
        Device {
            name: name.to_string(),
            range,
            
            callback,
        }
    }
    
    pub fn match_memory(&mut self, msg: MatchMsg) -> bool {
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

    pub fn read(&mut self, addr: u32, mask: super::Mask) -> u32 {
        if let Some(callback) = self.callback.as_mut() {
            return callback(AttchDirection::Read, addr - self.range.start, None, mask);
        }
        0
    }

    pub fn write(&mut self, addr: u32, data: u32, mask: super::Mask) {
        if let Some(callback) = self.callback.as_mut() {
            callback(AttchDirection::Write, addr - self.range.start, Some(data), mask);
        }
    }
}

#[repr(u32)]
pub enum SerialReg {
    TXR = 0x3f8,
}

impl From<u32> for SerialReg {
    fn from(val: u32) -> Self {
        match val {
            0x3f8 => SerialReg::TXR,
            _ => panic!("Invalid SerialReg value: {:x}", val),
        }
    }
}

pub struct SerialFactory;

impl SerialFactory {
    pub fn new(base: u32) -> Device {
        let mut regs = HashMap::new();
        regs.insert(0x000003f8, 0);
        
        let mut serial = Device::new("Serial", (base)..(base+0x1000), None);
        serial.callback = Some(Box::new(move |dir, addr, data, _| {
            match dir {
                AttchDirection::Read => {
                    match SerialReg::from(addr) {
                        SerialReg::TXR => {
                            return regs.get(&addr).unwrap().clone();
                        }
                    }
                }
                AttchDirection::Write => {
                    match SerialReg::from(addr) {
                        SerialReg::TXR => {
                            print!("{}", data.unwrap() as u8 as char);
                            regs.insert(addr, data.unwrap());
                        }
                    }
                }
            }
            0
        }));

        serial
    }
}
