
pub struct Memory {
    pub name: String,
    pub base: u32,
    pub memory: Box<[u8]>,
}

use super::{Mask, MMT};
use super::super::simErr;

impl Memory {
    pub fn new(name: &str, base: u32, size: u32) -> Self {
        Memory {
            name: name.to_string(),
            base,
            memory: vec![0; size.try_into().unwrap()].into_boxed_slice(),
        }
    }
}

impl MMT for Memory {
    fn match_memory(&mut self, msg: super::MatchMsg) -> bool {
        match msg {
            super::MatchMsg::ADDR { addr } => {
                if addr >= self.base && addr < self.base + self.memory.len() as u32 {
                    true
                } else {
                    false
                }
            }
            super::MatchMsg::NAME { name } => {
                if self.name == name {
                    true
                } else {
                    false
                }
            }
        }
    }

    fn read(&mut self, addr: u32, mask: Mask) -> u32 {
        let offset = (addr - self.base) as usize;
        match mask {
            Mask::Byte => {
                self.memory[offset] as u32
            }
            Mask::Half => {
                u32::from_le_bytes(self.memory[offset..offset + 2].try_into().unwrap())
            }
            Mask::Word => {
                u32::from_le_bytes(self.memory[offset..offset + 4].try_into().unwrap())
            }
            Mask::None => {
                u32::from_le_bytes(self.memory[offset..offset + 4].try_into().unwrap())
            }
        }
    }

    fn write(&mut self, addr: u32, data: u32, mask: Mask) {
        let offset = (addr - self.base) as usize;
        match mask {
            Mask::Byte => {
                self.memory[offset] = data as u8;
            }
            Mask::Half => {
                self.memory[offset..offset + 2].copy_from_slice(&data.to_le_bytes()[..2]);
            }
            Mask::Word => {
                self.memory[offset..offset + 4].copy_from_slice(&data.to_le_bytes());
            }
            Mask::None => {
                self.memory[offset..offset + 4].copy_from_slice(&data.to_le_bytes());
            }
        }
    }

    fn load(&mut self, data: &[u8]) -> Result<(), crate::SimulatorError> {
        if data.len() > self.memory.len() {
            return Err(simErr::NoMatchingMemory { msg: super::MatchMsg::NAME { name: format!("Too long bin for {}", self.name) } });
        }

        self.memory[..data.len()].copy_from_slice(data);
        Ok(())
    }

    fn get_memory(&mut self) -> &mut [u8] {
        &mut self.memory
    }
}
