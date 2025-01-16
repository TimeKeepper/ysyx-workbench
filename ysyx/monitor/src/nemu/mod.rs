use std::os::raw::c_void;

use crate::{mmu::MMU, simulator};

ysyx_macro::mod_pub!(decode, execute);
use circular_queue::CircularQueue;
use owo_colors::OwoColorize;
use rustyline::line_buffer::Direction;

use super::disassembler;
use super::differtest;

pub struct Simulator {
    pub decoder: decode::Decoder,
    pub executer: execute::Executor,

    pub mmu: MMU,

    pub inst_trace_buffer: (bool, CircularQueue<ExecuteInst>),
    pub inst_trace: bool,
    
    pub disasm: disassembler::Disassembler,
    pub differtest: differtest::Differtest,
}

impl Simulator{
    pub fn new() -> Self {
        let mut mmu = MMU::new();
        mmu.add_memory("sram",  0x0f00_0000, 0x0000_2000);
        mmu.add_memory("mrom",  0x2000_0000, 0x0000_1000);
        mmu.add_memory("flash", 0x3000_0000, 0x1000_0000);
        mmu.add_memory("psram", 0x8000_0000, 0x0800_0000);
        mmu.add_memory("sdram", 0xa000_0000, 0x0200_0000);

        Self {
            decoder: decode::Decoder::new(),
            executer: execute::Executor::new(0x3000_0000),

            mmu,
            inst_trace_buffer: (false, CircularQueue::with_capacity(10)),
            inst_trace: false,
            disasm: disassembler::Disassembler::new("riscv64-unknown-linux-gnu"),
            differtest: differtest::Differtest::new().unwrap(),
        }
    }

    pub fn init(&mut self, bin_path: Option<String>) -> Result<(), simulator::SimulatorError> {
        if bin_path.is_none() { return Err(simulator::SimulatorError::NoBinaryFile) };
        let bin = std::fs::read(bin_path.unwrap());
        if bin.is_err() { return Err(simulator::SimulatorError::BinaryFileNotFound) };
        let bin = bin.unwrap();
        self.mmu.load("flash", &bin).unwrap();
        self.differtest.ref_difftest_init(1234);
        self.differtest.ref_difftest_memcpy(0x3000_0000, self.mmu.match_memory_by_addr(0x3000_0000).ok().unwrap().memory.as_mut_ptr() as *mut c_void, bin.len() as u64, differtest::DiffertestDirection::ToRef);
        return Ok(());
    }

    fn execute(&mut self, inst: ExecuteInst) -> Result<(), simulator::SimulatorError> {
        self.executer.execute(inst.clone())?;

        if self.inst_trace_buffer.0 {
            self.inst_trace_buffer.1.push(inst.clone());
        }
        
        Ok(())
    }

    fn disasm(&self, inst: u32) {
        let result = self.disasm
            .disasm(&inst.to_le_bytes(), self.executer.pc.value as u64)
            .replace("\0", "")
            .trim()
            .split_ascii_whitespace()
            .map(|x| format!("{} ", x))
            .collect::<String>();
        println!("{:08x}: {:08x} {}", self.executer.pc.value.purple(), inst.red(), result.green());
    }
}

impl simulator::Simulator for Simulator {

    fn single_instruction(&mut self, time: u32) -> Result<simulator::SimulatorOk, simulator::SimulatorError> {
        for _ in 0..time {
            let addr = self.executer.pc.value;
            let inst = self.mmu.read(addr)?;
            if time < 10 && self.inst_trace {
                self.disasm(inst);
            }
            let inst = self.decoder.decode(inst)?;
            println!("{:?}", inst);
            self.execute(inst)?;
        }
        Ok(simulator::SimulatorOk::InstructionExecuted)
    }
}

#[derive(Debug, PartialEq, Clone)]
pub struct ExecuteInst {
    pub name: String,
    pub rs1: u8,
    pub rs2: u8,
    pub rd: u8,
    pub imm: u32,
}

impl ExecuteInst {
    pub fn new(name: &str, rs1: u8, rs2: u8, rd: u8, imm: u32) -> Self {
        Self {
            name: name.to_string(),
            rs1,
            rs2,
            rd,
            imm,
        }
    }
}
