use std::fmt::Display;

#[derive(Debug, PartialEq, Clone)]
pub enum MatchMsg {
    ADDR {addr: u32},
    NAME {name: String},
}

impl Display for MatchMsg {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            MatchMsg::ADDR {addr} => write!(f, "ADDR: 0x{:08x}", addr),
            MatchMsg::NAME {name} => write!(f, "NAME: {}", name),
        }
    }
}

#[derive(Debug, PartialEq, Clone)]
#[repr(u8)]
pub enum Mask{
    None,
    Byte = 1,
    Half = 2,
    Word = 4,
}

impl Mask {
    pub fn transform(&self, data: u32) -> u32 {
        match self {
            Mask::Byte => data & 0xFF,
            Mask::Half => data & 0xFFFF,
            Mask::Word => data,
            Mask::None => data,
        }
    }
}
