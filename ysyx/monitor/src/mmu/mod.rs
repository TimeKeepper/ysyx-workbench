use crate::simulator::SimulatorError;

struct MemoryManagementUnit {
    name: String,
    base: u32,
    memory: Box<[u8]>,
}

pub enum Mask{
    Byte,
    Half,
    Word,
}

impl MemoryManagementUnit {
    fn new(name: &str, base: u32, size: u32) -> Self {
        MemoryManagementUnit {
            name: name.to_string(),
            base,
            memory: vec![0; size.try_into().unwrap()].into_boxed_slice(),
        }
    }

    fn read(&self, addr: u32) -> u32 {
        if addr % 4 != 0 {
            panic!("Unaligned memory access");
        }
        let offset = (addr - self.base) as usize;
        u32::from_le_bytes(self.memory[offset..offset + 4].try_into().unwrap())
    }

    fn write(&mut self, addr: u32, data: u32, mask: Mask) {
        if addr % 4 != 0 {
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

pub struct MMU {
    memory: Vec<MemoryManagementUnit>,
}

impl MMU {
    pub fn new() -> Self {
        MMU {
            memory: Vec::new(),
        }
    }

    pub fn add_memory(&mut self, name: &str, base: u32, size: u32) {
        self.memory.push(MemoryManagementUnit::new(name, base, size));
    }

    fn match_memory(&mut self, addr: u32) -> Result<&mut MemoryManagementUnit, SimulatorError> {
        for i in &mut self.memory {
            if addr >= i.base && addr < i.base + i.memory.len() as u32 {
                return Ok(i);
            }
        }
        Err(SimulatorError::NoMatchingMemory)
    }

    pub fn read(&mut self, addr: u32) -> Result<u32, SimulatorError> {
        let memory = self.match_memory(addr)?;
        Ok(memory.read(addr))
    }

    pub fn write(&mut self, addr: u32, data: u32, mask: Mask) -> Result<(), SimulatorError> {
        let memory = self.match_memory(addr)?;
        memory.write(addr, data, mask);
        Ok(())
    }
}