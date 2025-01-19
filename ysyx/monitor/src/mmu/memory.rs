
pub struct Memory {
    pub name: String,
    pub base: u32,
    pub memory: Box<[u8]>,
}

#[derive(Debug, PartialEq, Clone)]
#[repr(u8)]
pub enum Mask{
    Byte = 1,
    Half = 2,
    Word = 4,
}

impl Memory {
    pub fn new(name: &str, base: u32, size: u32) -> Self {
        Memory {
            name: name.to_string(),
            base,
            memory: vec![0; size.try_into().unwrap()].into_boxed_slice(),
        }
    }

    pub fn read(&self, addr: u32) -> u32 {
        if addr % 4 != 0 {
            panic!("Unaligned memory access");
        }
        let offset = (addr - self.base) as usize;
        u32::from_le_bytes(self.memory[offset..offset + 4].try_into().unwrap())
    }

    pub fn read_with_mask(&self, addr: u32, mask: Mask) -> u32 {
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
        }
    }

    pub fn write(&mut self, addr: u32, data: u32, mask: Mask) {
        if addr % mask.clone() as u32 != 0 {
            panic!("Unaligned memory access");
        }
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
        }
    }
}
