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
