ysyx_macro::mod_flat!(riscv, loongarch);

use std::sync::{Arc, Mutex};

use msg_resp::{ResultMessage, SimErr};

pub enum RegIdentifier<'a> {
    Index(usize),
    Name(&'a str),
}

pub trait RegisterOps {
    fn read_gpr(&self, reg: RegIdentifier) -> Result<u32, SimErr>;
    fn write_gpr(&mut self, reg: RegIdentifier, value: u32) -> ResultMessage;
    fn print_gpr(&self, specify: Option<String>) -> ResultMessage;

    fn read_pc(&self) -> u32;
    fn write_pc(&mut self, value: u32);

    fn read_csr(&self, reg: RegIdentifier) -> Result<u32, SimErr>;
    fn write_csr(&mut self, reg: RegIdentifier, value: u32) -> ResultMessage;
    fn print_csr(&self, specify: Option<String>) -> ResultMessage;
}

#[cfg(feature = "riscv32")]
pub type RegisterBank = riscv::RegisterBank;
