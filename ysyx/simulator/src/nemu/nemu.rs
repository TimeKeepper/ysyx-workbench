use circular_queue::CircularQueue;
use super::decode::RvInstParser;
use owo_colors::OwoColorize;

use super::super::disassembler;

use msg_resp::{SimErr, SimOk, ResultMessage, CtrlCommand};

use std::sync::mpsc::Receiver;
use std::sync::mpsc::Sender;
use std::sync::{Arc, Mutex};

use state::ProcessState;
use state::mmu::MMU;
use state::reg::RegisterBank;

pub struct Simulator {
    pub inst_parser: RvInstParser,

    pub inst_trace_buffer: (bool, CircularQueue<ExecuteInst>),
    pub inst_trace: bool,
    pub execte_times: u32,
    
    pub disasm: disassembler::Disassembler,
    
    pub cmd_receiver: Receiver<CtrlCommand>,
    pub result_sender: Sender<ResultMessage>,

    pub mem: Arc<Mutex<MMU>>,
    pub reg: Arc<Mutex<RegisterBank>>,
    pub state: Arc<Mutex<ProcessState>>,
}

impl Simulator{
    pub fn new(cmd_receiver: Receiver<CtrlCommand>, result_sender: Sender<ResultMessage>, 
        mem: Arc<Mutex<state::mmu::MMU>>, 
        reg: Arc<Mutex<state::reg::RegisterBank>>,
        state: Arc<Mutex<ProcessState>>) -> Self {

        Self {
            inst_parser: RvInstParser::new(),

            inst_trace_buffer: (false, CircularQueue::with_capacity(10)),
            inst_trace: false,
            execte_times: 0,

            disasm: disassembler::Disassembler::new("riscv64-unknown-linux-gnu"),

            cmd_receiver,
            result_sender,

            mem,
            reg,
            state,
        }
    }

    fn decode(&self, inst: u32) -> Result<ExecuteInst, SimErr> {
        self.inst_parser.parse(inst)
    }

    fn disasm(&self, inst: u32) {
        let pc = self.reg.lock().unwrap().pc;
        
        let result = self.disasm
            .disasm(&inst.to_le_bytes(), pc as u64)
            .replace("\0", "")
            .trim()
            .split_ascii_whitespace()
            .map(|x| format!("{} ", x))
            .collect::<String>();
        println!("{:08x}: {:08x} {}", pc.purple(), inst.red(), result.green());
    }
}

// impl simulator::Simulator for Simulator {
//     fn single_instruction(&mut self, trace: bool) -> ResultMessage {
//         let addr: u32 = self.cpu_state.pc;
//         let inst = self.mmu.read(addr, crate::mmu::Mask::None)?;
//         let exeu_inst = self.decode(inst)?;
        
//         if self.inst_trace && trace {
//             self.disasm(inst);
//             println!("{:08x?}", exeu_inst.green());
//         }

//         self.execute(exeu_inst)?;
        
//         self.execte_times += 1;

//         Ok(SimOk::InstructionExecuted)
//     }
    
//     fn instruction_ring_buffer(&mut self) {
//         for i in self.inst_trace_buffer.1.iter().rev() {
//             println!("{:?}", i);
//         }
//     }
    
//     fn times(&mut self) {
//         println!("Execute times: {}", self.execte_times.purple());
//     }
    
//     fn external_state_change(&mut self) -> bool {
//         self.mmu.is_attch_device == false
//     }

//     fn get_reg_state(&self) -> &Riscv32CpuState {
//         &self.cpu_state
//     }

//     fn get_mem_state(&mut self) -> &mut MMU {
//         &mut self.mmu
//     }
    
//     fn state(&mut self, target: String, specify: Option<String>) -> ResultMessage {
//         match target.as_str() {
//             "gpr" => {
//                 if specify.is_none() {
//                     for (i, r) in self.cpu_state.gpr.iter().enumerate() {
//                         println!("{}: \t0x{:08x}", RV32GPR_NAME[i].purple(), r.red());
//                     }
//                     return Ok(SimOk::Nothing);
//                 }

//                 let specify = specify.unwrap();
                
//                 let index = specify.parse::<u32>();
//                 if index.is_ok() && (0..32).contains(&index.clone().unwrap()) {
//                     let index = index.unwrap();
//                     println!("{}: \t0x{:08x}", specify.purple(), self.cpu_state.gpr[index as usize].red());
//                     return Ok(SimOk::Nothing);
//                 }
                
//                 for name in RV32GPR_NAME.iter() {
//                     if name == &specify {
//                         println!("{}: \t0x{:08x}", specify.purple(), self.cpu_state.gpr[RV32GPR_NAME.iter().position(|&r| r == specify).unwrap()].red());
//                         return Ok(SimOk::Nothing);
//                     }
                    
//                 }

//                 println!("Invalid index");
//                 return Err(SimErr::InvalidCommand);
//             }

//             "pc" => {
//                 println!("{}: \t0x{:08x}", "pc".purple(), self.cpu_state.pc.red());
//                 return Ok(SimOk::Nothing);
//             }

//             "csr" => {
//                 if specify.is_none() {
//                     println!("{}", "You Have to specify csr index".red());
//                     return Err(SimErr::InvalidCommand);
//                 }

//                 let specify = specify.unwrap();

//                 let index = specify.parse::<u32>();

//                 if index.is_err() {
//                     println!("{}", "index parse error(to u32)".red());
//                     return Err(SimErr::InvalidCommand);
//                 }

//                 let index = index.unwrap();
//                 if !(0..4096).contains(&index) {
//                     println!("{}", "Invalid index, should be in 0..4096".red());
//                     return Err(SimErr::InvalidCommand);
//                 }

//                 println!("{}{}: \t0x{:08x}", "csr".purple(), index.red(), self.cpu_state.csr[index as usize].red());

//                 return Ok(SimOk::Nothing);
//             }

//             _ => {
//                 println!("Invalid target");
//                 return Err(SimErr::InvalidCommand);
//             }
//         }
//     }

//     fn func_ctrl(&mut self, on_or_off: bool, target: Option<&str>) -> ResultMessage {
//         if target.is_none() {
//             function_log("instruction trace", self.inst_trace);
//             function_log("instruction trace buffer", self.inst_trace_buffer.0);
//             return Ok(SimOk::Nothing);
//         }
        
//         let target: &str = &target.unwrap();

//         match target {
//             "it" => {
//                 self.inst_trace = on_or_off;
//                 function_log("Instruction trace", on_or_off);
//                 return Ok(SimOk::Nothing);
//             }
//             "ir" => {
//                 self.inst_trace_buffer.0 = on_or_off;
//                 function_log("Instruction trace buffer", on_or_off);
//                 return Ok(SimOk::Nothing);
//             }
//             _ => {
//                 println!("{}", "You should input valid target from [it, ir]".red());
//                 return Err(SimErr::InvalidCommand);
//             }
//         }
//     }
// }

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
