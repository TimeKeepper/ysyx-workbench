use crate::simulator::SimulatorError;

pub struct MemoryManagementUnit {
    name: String,
    base: u32,
    pub memory: Box<[u8]>,
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

    pub fn match_memory_by_addr(&mut self, addr: u32) -> Result<&mut MemoryManagementUnit, SimulatorError> {
        for i in &mut self.memory {
            if addr >= i.base && addr < i.base + i.memory.len() as u32 {
                return Ok(i);
            }
        }
        Err(SimulatorError::NoMatchingMemoryByAddress { addr: (addr) })
    }

    pub fn match_memory_by_name(&mut self, name: &str) -> Result<&mut MemoryManagementUnit, SimulatorError> {
        for i in &mut self.memory {
            if i.name == name {
                return Ok(i);
            }
        }
        Err(SimulatorError::NoMatchingMemoryByName { name: name.to_string() })
    }

    pub fn read(&mut self, addr: u32) -> Result<u32, SimulatorError> {
        let memory = self.match_memory_by_addr(addr)?;
        Ok(memory.read(addr))
    }

    pub fn write(&mut self, addr: u32, data: u32, mask: Mask) -> Result<(), SimulatorError> {
        let memory = self.match_memory_by_addr(addr)?;
        memory.write(addr, data, mask);
        Ok(())
    }

    pub fn load(&mut self, name: &str, data: &[u8]) -> Result<(), SimulatorError>  {
        let memory = self.match_memory_by_name(name)?;

        if data.len() > memory.memory.len() {
            return Err(SimulatorError::NoMatchingMemoryByName { name: format!("Too long bin for {}", name) });
        }

        memory.memory[..data.len()].copy_from_slice(data);
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_mmu() {
        let bin_path = "src/test/rtthread-riscv32e-ysyxsoc.bin";
        let mem = std::fs::read(bin_path).unwrap();

        let mut mmu = MMU::new();
        mmu.add_memory("sdram", 0x8000_0000, 0x0800_0000);
        assert!(mmu.load("sdram", &mem).is_ok());
        println!("length: {}", mem.len());
        for i in 0..10 {
            println!("sdram: \t0x{:08x}", mmu.read(0x8000_0000 + 4*i).unwrap());
            println!("mem: \t0x{:08x}", u32::from_be_bytes(mem[(4*i as usize)..(4*i as usize + 4)].try_into().unwrap()));
        }
    }
}