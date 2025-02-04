use owo_colors::OwoColorize;

use super::memory::Memory;
use super::{devices::Device, Mask};
use msg_resp::{MatchMsg, SimErr};

use std::sync::{Arc, Mutex};

use msg_resp as msgr;

pub enum MMT<'a> {
    Memory(&'a Memory),
    Device(&'a Device),
}

pub struct MMU {
    memory: Vec<Memory>,
    device: Vec<Device>,

    pub resper: Arc<Mutex<msgr::Resper>>,
}

impl MMU {
    pub fn new(resper: Arc<Mutex<msgr::Resper>>) -> Self {
        MMU {
            memory: Vec::new(),
            device: Vec::new(),
            resper,
        }
    }

    pub fn add_memory(&mut self, name: &str, base: u32, size: u32) {
        self.memory.push(Memory::new(name, base, size));
    }

    pub fn add_device(&mut self, device: Device) {
        self.device.push(device);
    }

    pub fn match_memory(&self, msg: MatchMsg) -> Result<&Memory, SimErr> {
        for memory in self.memory.iter() {
            if memory.match_memory(msg.clone()) {
                return Ok(memory);
            }
        }

        self.resper.lock().unwrap().error(format!("No matching memory: {}", match msg {
            MatchMsg::ADDR { addr } => format!("0x{:08x}", addr),
            MatchMsg::NAME { name } => name,
        }).as_str());

        Err(SimErr::NoMatchingMemory)
    }

    pub fn match_memory_mut(&mut self, msg: MatchMsg) -> Result<&mut Memory, SimErr> {
        for memory in self.memory.iter_mut() {
            if memory.match_memory(msg.clone()) {
                return Ok(memory);
            }
        }

        self.resper.lock().unwrap().error(format!("No matching memory: {}", match msg {
            MatchMsg::ADDR { addr } => format!("0x{:08x}", addr),
            MatchMsg::NAME { name } => name,
        }).as_str());

        Err(SimErr::NoMatchingDevice)
    }

    pub fn match_device(&mut self, msg: MatchMsg) -> Result<&mut Device, SimErr> { // Device always mutable
        for device in self.device.iter_mut() {
            if device.match_memory(msg.clone()) {
                return Ok(device);
            }
        }

        self.resper.lock().unwrap().error(format!("No matching device: {}", match msg {
            MatchMsg::ADDR { addr } => format!("0x{:08x}", addr),
            MatchMsg::NAME { name } => name,
        }).as_str());

        Err(SimErr::NoMatchingDevice)
    }

    pub fn read(&self, addr: u32, mask: Mask) -> Result<u32, SimErr> {
        let memory = self.match_memory(MatchMsg::ADDR { addr: addr as u32 })?;
        Ok(memory.read(addr as u32, mask))
    }

    pub fn write(&mut self, addr: u32, data: u32, mask: Mask) -> Result<(), SimErr> {
        let memory = self.match_memory_mut(MatchMsg::ADDR { addr: addr as u32 })?;
        memory.write(addr as u32, data, mask);
        Ok(())
    }

    pub fn read_device(&mut self, addr: u32, mask: Mask) -> Result<u32, SimErr> {
        let device = self.match_device(MatchMsg::ADDR { addr: addr as u32 })?;
        Ok(device.read(addr as u32, mask))
    }

    pub fn write_device(&mut self, addr: u32, data: u32, mask: Mask) -> Result<(), SimErr> {
        let device = self.match_device(MatchMsg::ADDR { addr: addr as u32 })?;
        device.write(addr as u32, data, mask);
        Ok(())
    }

    pub fn load(&mut self, name: &str, data: &[u8]) -> Result<(), SimErr> {
        let memory = self.match_memory_mut(MatchMsg::NAME {
            name: name.to_string(),
        })?;

        memory.load(data).map_err(|e| {
            self.resper.lock().unwrap().error(format!("{}: {}", "can not load memory for too long size", name.purple()).as_str());
            e
        })
    }

    pub fn memory_map(&self) {
        println!("{}", "Memory Map:".purple());
        for m in &self.memory {
            println!(
                "{}: \t0x{:08x} - 0x{:08x}",
                m.name.red(),
                m.base.green(),
                (m.base + m.memory.len() as u32).green()
            );
        }
        println!("{}", "Device Map:".purple());
        for d in &self.device {
            println!(
                "{}: \t0x{:08x} - 0x{:08x}",
                d.name.red(),
                d.range.start.green(),
                d.range.end.green()
            );
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

        let mut mmu = MMU::new(Arc::new(Mutex::new(msgr::Resper::new())));
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
