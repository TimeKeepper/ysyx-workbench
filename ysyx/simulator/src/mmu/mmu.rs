use super::super::simErr;
use super::memory::Memory;
use super::{Device, Mask, MatchMsg};

pub enum MMT<'a> {
    Memory(&'a mut Memory),
    Device(&'a mut Device),
}

impl<'a> MMT<'a> {
    pub fn read(&mut self, addr: u32, mask: Mask) -> u32 {
        match self {
            MMT::Memory(memory) => memory.read(addr, mask),
            MMT::Device(device) => device.read(addr, mask),
        }
    }

    pub fn write(&mut self, addr: u32, data: u32, mask: Mask) {
        match self {
            MMT::Memory(memory) => memory.write(addr, data, mask),
            MMT::Device(device) => device.write(addr, data, mask),
        }
    }
}

pub struct MMU {
    memory: Vec<Memory>,
    device: Vec<Device>,

    pub is_attch_device: bool,
}

impl MMU {
    pub fn new() -> Self {
        MMU {
            memory: Vec::new(),
            device: Vec::new(),

            is_attch_device: false,
        }
    }

    pub fn add_memory(&mut self, name: &str, base: u32, size: u32) {
        self.memory.push(Memory::new(name, base, size));
    }

    pub fn add_device(&mut self, device: Device) {
        self.device.push(device);
    }

    pub fn match_memory(&mut self, msg: MatchMsg) -> Result<MMT, simErr> {
        for memory in self.memory.iter_mut() {
            if memory.match_memory(msg.clone()) {
                self.is_attch_device = false;
                return Ok(MMT::Memory(memory));
            }
        }

        for device in self.device.iter_mut() {
            if device.match_memory(msg.clone()) {
                self.is_attch_device = true;
                return Ok(MMT::Device(device));
            }
        }

        Err(simErr::NoMatchingMemory { msg })
    }

    pub fn read(&mut self, addr: u32, mask: Mask) -> Result<u32, simErr> {
        let mut memory = self.match_memory(MatchMsg::ADDR { addr: addr as u32 })?;
        Ok(memory.read(addr as u32, mask))
    }

    pub fn write(&mut self, addr: u32, data: u32, mask: Mask) -> Result<(), simErr> {
        let mut memory = self.match_memory(MatchMsg::ADDR { addr: addr as u32 })?;
        memory.write(addr as u32, data, mask);
        Ok(())
    }

    pub fn load(&mut self, name: &str, data: &[u8]) -> Result<(), simErr> {
        let memory = self.match_memory(MatchMsg::NAME {
            name: name.to_string(),
        })?;

        match memory {
            MMT::Memory(memory) => {
                memory.load(data)?;
                Ok(())
            }
            MMT::Device(device) => Err(simErr::DeviceCannotBeLoad {
                name: device.name.clone(),
            }),
        }
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
            println!(
                "sdram: \t0x{:08x}",
                mmu.read(0x8000_0000 + 4 * i, Mask::None).unwrap()
            );
            println!(
                "mem: \t0x{:08x}",
                u32::from_be_bytes(
                    mem[(4 * i as usize)..(4 * i as usize + 4)]
                        .try_into()
                        .unwrap()
                )
            );
        }
    }
}
