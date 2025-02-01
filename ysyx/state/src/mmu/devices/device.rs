use std::ops::Range;

use msg_resp::MatchMsg;

use super::super::Mask;

#[derive(Debug, PartialEq, Clone)]
pub enum AttchDirection {
    Read,
    Write,
}

pub struct Device {
    pub name: String,
    pub range: Range<u32>,

    pub callback: Option<Box<dyn FnMut(AttchDirection, u32, Option<u32>, Mask) -> u32>>
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

    pub fn read(&mut self, addr: u32, mask: Mask) -> u32 {
        if let Some(callback) = self.callback.as_mut() {
            return callback(AttchDirection::Read, addr - self.range.start, None, mask);
        }
        0
    }

    pub fn write(&mut self, addr: u32, data: u32, mask: Mask) {
        if let Some(callback) = self.callback.as_mut() {
            callback(AttchDirection::Write, addr - self.range.start, Some(data), mask);
        }
    }
}
