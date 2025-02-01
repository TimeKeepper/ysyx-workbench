ysyx_macro::mod_flat!(riscv, loongarch);

use std::sync::{Arc, Mutex};

pub enum RegIdentifier<'a> {
    Index(usize),
    Name(&'a str),
}

pub trait RegisterOps {
    fn read_gpr(&self, reg: RegIdentifier) -> Result<u32, ()>;
    fn write_gpr(&mut self, reg: RegIdentifier, value: u32) -> Result<(), ()>;
    fn read_pc(&self) -> u32;
    fn write_pc(&mut self, value: u32);
}

#[cfg(feature = "riscv32")]
pub type RegisterBank = riscv::RegisterBank;
