use std::collections::HashMap;

use super::{AttchDirection, Device};
enum SerialReg {
    TXR,
}

impl From<u32> for SerialReg {
    fn from(val: u32) -> Self {
        match val {
            0x000 => SerialReg::TXR,
            _ => panic!("Invalid SerialReg value: {:x}", val),
        }
    }
}

pub struct SerialFactory;

impl SerialFactory {
    pub fn new(base: u32) -> Device {
        let mut regs = HashMap::new();
        regs.insert(0x00000000, 0);
        
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
