use super::memory::Memory;
use super::{Mask, MatchMsg, MMT};
use super::super::simErr;

pub struct MMU {
    memory: Vec<Box<dyn MMT>>,
}

impl MMU {
    pub fn new() -> Self {
        MMU {
            memory: Vec::new(),
        }
    }

    pub fn add_memory(&mut self, name: &str, base: u32, size: u32) {
        self.memory.push(Box::new(Memory::new(name, base, size)));
    }

    pub fn match_memory(&mut self, msg: MatchMsg) -> Result<&mut dyn MMT, simErr> {
        for memory in self.memory.iter_mut() {
            if memory.match_memory(msg.clone()) {
                return Ok(memory.as_mut());
            }
        }
        Err(simErr::NoMatchingMemory { msg })
    }

    pub fn read(&mut self, addr: u32, mask: Mask) -> Result<u32, simErr> {
        let memory = self.match_memory(MatchMsg::ADDR { addr: addr as u32 })?;
        Ok(memory.read(addr as u32, mask))
    }

    pub fn write(&mut self, addr: u32, data: u32, mask: Mask) -> Result<(), simErr> {
        let memory = self.match_memory(MatchMsg::ADDR { addr: addr as u32 })?;
        memory.write(addr as u32, data, mask);
        Ok(())
    }

    pub fn load(&mut self, name: &str, data: &[u8]) -> Result<(), simErr> {
        let memory = self.match_memory(MatchMsg::NAME { name: name.to_string() })?;

        memory.load(data)
        // if data.len() > memory.get_range().len() {
        //     return Err(simErr::NoMatchingMemory { msg: MatchMsg::NAME { name: format!("Too long bin for {}", name) } });
        // }

        // memory.get_memory()[..data.len()].copy_from_slice(data);
        // Ok(())
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
            println!("sdram: \t0x{:08x}", mmu.read(0x8000_0000 + 4*i, Mask::None).unwrap());
            println!("mem: \t0x{:08x}", u32::from_be_bytes(mem[(4*i as usize)..(4*i as usize + 4)].try_into().unwrap()));
        }
    }
}