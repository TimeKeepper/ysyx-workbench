use super::memory::{Memory, Mask};
use super::super::simErr;

pub struct MMU {
    memory: Vec<Memory>,
}

impl MMU {
    pub fn new() -> Self {
        MMU {
            memory: Vec::new(),
        }
    }

    pub fn add_memory(&mut self, name: &str, base: u32, size: u32) {
        self.memory.push(Memory::new(name, base, size));
    }

    pub fn match_memory_by_addr(&mut self, addr: u32) -> Result<&mut Memory, simErr> {
        for i in &mut self.memory {
            if addr >= i.base && addr < i.base + i.memory.len() as u32 {
                return Ok(i);
            }
        }
        Err(simErr::NoMatchingMemoryByAddress { addr: (addr) })
    }

    pub fn match_memory_by_name(&mut self, name: &str) -> Result<&mut Memory, simErr> {
        for i in &mut self.memory {
            if i.name == name {
                return Ok(i);
            }
        }
        Err(simErr::NoMatchingMemoryByName { name: name.to_string() })
    }

    pub fn read(&mut self, addr: u32) -> Result<u32, simErr> { // read have not mask in hardware
        let memory = self.match_memory_by_addr(addr)?;
        Ok(memory.read(addr))
    }

    pub fn read_with_mask(&mut self, addr: u32, mask: Mask) -> Result<u32, simErr> {
        let memory = self.match_memory_by_addr(addr)?;
        Ok(memory.read_with_mask(addr, mask))
    }

    pub fn write(&mut self, addr: u32, data: u32, mask: Mask) -> Result<(), simErr> {
        let memory = self.match_memory_by_addr(addr)?;
        memory.write(addr, data, mask);
        Ok(())
    }

    pub fn load(&mut self, name: &str, data: &[u8]) -> Result<(), simErr>  {
        let memory = self.match_memory_by_name(name)?;

        if data.len() > memory.memory.len() {
            return Err(simErr::NoMatchingMemoryByName { name: format!("Too long bin for {}", name) });
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