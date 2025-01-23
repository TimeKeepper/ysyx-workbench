use crate::mmu::devices::{SerialFactory, TimerFactory};
use crate::{mmu::MMU, simulator};

use circular_queue::CircularQueue;
use super::decode::RvInstParser;
use super::state::Riscv32CpuState;
use owo_colors::OwoColorize;

use super::super::disassembler;

pub struct Simulator {
    pub inst_parser: RvInstParser,
    pub cpu_state: Riscv32CpuState,

    pub mmu: MMU,

    pub inst_trace_buffer: (bool, CircularQueue<ExecuteInst>),
    pub inst_trace: bool,
    pub execte_times: u32,
    
    pub disasm: disassembler::Disassembler,
}

impl Simulator{
    pub fn new() -> Self {
        let mut mmu = MMU::new();
        mmu.add_memory("sram",  0x0f00_0000, 0x0000_2000);
        mmu.add_memory("mrom",  0x2000_0000, 0x0000_1000);
        mmu.add_memory("flash", 0x3000_0000, 0x1000_0000);
        mmu.add_memory("psram", 0x8000_0000, 0x0800_0000);
        mmu.add_memory("sdram", 0xa000_0000, 0x0200_0000);

        mmu.add_device(SerialFactory::new(0x1000_0000));
        mmu.add_device(TimerFactory::new(0x1000_2000));

        Self {
            inst_parser: RvInstParser::new(),
            cpu_state: Riscv32CpuState::new(0x8000_0000),

            mmu,

            inst_trace_buffer: (false, CircularQueue::with_capacity(10)),
            inst_trace: false,
            execte_times: 0,

            disasm: disassembler::Disassembler::new("riscv64-unknown-linux-gnu"),
        }
    }

    fn decode(&self, inst: u32) -> Result<ExecuteInst, simulator::SimulatorError> {
        self.inst_parser.parse(inst)
    }

    fn disasm(&self, inst: u32) {
        let result = self.disasm
            .disasm(&inst.to_le_bytes(), self.cpu_state.pc.value as u64)
            .replace("\0", "")
            .trim()
            .split_ascii_whitespace()
            .map(|x| format!("{} ", x))
            .collect::<String>();
        println!("{:08x}: {:08x} {}", self.cpu_state.pc.value.purple(), inst.red(), result.green());
    }
}

impl simulator::Simulator for Simulator {
    fn single_instruction(&mut self) -> Result<simulator::SimulatorOk, simulator::SimulatorError> {
        let addr: u32 = self.cpu_state.pc.value;
        let inst = self.mmu.read(addr, crate::mmu::Mask::None)?;
        let exeu_inst = self.decode(inst)?;
        
        if self.inst_trace {
            self.disasm(inst);
            println!("{:08x?}", exeu_inst.green());
        }

        self.execute(exeu_inst)?;
        
        self.execte_times += 1;

        Ok(simulator::SimulatorOk::InstructionExecuted)
    }
    
    fn instruction_ring_buffer(&mut self) {
        for i in self.inst_trace_buffer.1.iter().rev() {
            println!("{:?}", i);
        }
    }

    fn times(&mut self) {
        println!("Execute times: {}", self.execte_times.purple());
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
