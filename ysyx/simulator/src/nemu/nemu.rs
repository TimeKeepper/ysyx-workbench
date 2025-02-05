use circular_queue::CircularQueue;
use msg_resp::MatchMsg;
use state::reg::RegisterOps;
use ysyx_macro::with_rwlock_read;
use crate::differtest;

use super::decode::RvInstParser;
use owo_colors::OwoColorize;

use super::super::disassembler;

use msg_resp::{SimErr, SimOk, ResultMessage, CtrlCommand};

use std::result::Result;
use std::sync::mpsc::Receiver;
use std::sync::mpsc::RecvError;
use std::sync::mpsc::Sender;
use std::sync::{Arc, Mutex, RwLock};

use state::ProcessState;
use state::mmu::{MMU, Mask};
use state::reg::RegisterBank;

use msg_resp as msgr;

pub struct Simulator {
    pub inst_parser: RvInstParser,
    
    pub resper: Arc<Mutex<msgr::Resper>>,

    pub inst_trace_buffer: (bool, CircularQueue<ExecuteInst>),
    pub inst_trace: bool,
    pub execte_times: u32,
    
    pub disasm: disassembler::Disassembler,
    
    pub cmd_receiver: Receiver<CtrlCommand>,
    pub result_sender: Sender<ResultMessage>,

    pub mem: Arc<RwLock<MMU>>,
    pub reg: Arc<RwLock<RegisterBank>>,
    pub state: Arc<RwLock<ProcessState>>,
    
    #[cfg(feature = "differtest")]
    pub differtest: differtest::Differtest,
}

impl Simulator {
    pub fn new(cmd_receiver: Receiver<CtrlCommand>, result_sender: Sender<ResultMessage>, 
        mem: Arc<RwLock<state::mmu::MMU>>, 
        reg: Arc<RwLock<state::reg::RegisterBank>>,
        state: Arc<RwLock<ProcessState>>,
        resper: Arc<Mutex<msgr::Resper>>) -> Self {

        Self {
            inst_parser: RvInstParser::new(),

            resper,

            inst_trace_buffer: (false, CircularQueue::with_capacity(10)),
            inst_trace: false,
            execte_times: 0,

            disasm: disassembler::Disassembler::new("riscv64-unknown-linux-gnu"),

            cmd_receiver,
            result_sender,

            mem,
            reg,
            state,

            #[cfg(feature = "differtest")]
            differtest: differtest::Differtest::new(),
        }
    }

    fn decode(&self, inst: u32) -> Result<ExecuteInst, SimErr> {
        self.inst_parser.parse(inst)
    }

    fn disasm(&self, inst: u32) {
        let pc = with_rwlock_read!(self.reg, reg, {
            reg.read_pc()
        });

        let result = self.disasm
            .disasm(&inst.to_le_bytes(), pc as u64)
            .replace("\0", "")
            .trim()
            .split_ascii_whitespace()
            .map(|x| format!("{} ", x))
            .collect::<String>();
        self.resper.lock().unwrap().trace(format!("{:08x}: {:08x} {}", pc.purple(), inst.red(), result.green()).as_str());
    }

    fn single_instruction(&mut self, count: Option<u32>) -> ResultMessage {
        let mut orig = |trace: bool| -> Result<(), SimErr> {
            if !(self.state.read().unwrap().is_run()) {
                self.resper.lock().unwrap().important("The process is not running");
                return Err(SimErr::ExecuteInterrupt);
            }

            let inst = with_rwlock_read!(self.reg, reg, {
                with_rwlock_read!(self.mem, mem, {
                    mem.read(reg.read_pc(), Mask::None)?
                })
            });

            let exeu_inst = self.decode(inst)?;
    
            if trace && self.inst_trace {
                self.disasm(inst);
                self.resper.lock().unwrap().trace(format!("{:08x?}", exeu_inst).as_str());
            }
    
            self.execute(exeu_inst)?;
            
            self.execte_times += 1;
    
            Ok(())
        };

        let count = if count.is_none() { 1 } else { count.unwrap() };

        if count == 0 {
            loop {
                orig(false)?;
            }
        }

        for _ in 0..count {
            orig(count < 10)?;
        }

        Ok(SimOk::InstructionExecuted)
    }

    fn func_print(&mut self) {
        self.resper.lock().unwrap().option_log("Instruction trace", self.inst_trace);
        self.resper.lock().unwrap().option_log("Instruction trace buffer", self.inst_trace_buffer.0);
    }

    pub fn run(&mut self) {
        loop {
            let result = self.cmd_receiver.recv();

            match result {
                Ok(CtrlCommand::QUIT) => {
                    self.result_sender.send(Ok(SimOk::Quit)).unwrap();
                    break;
                }

                Ok(CtrlCommand::SI { count }) => {
                    let result = self.single_instruction(count);
                    self.result_sender.send(result).unwrap();
                }

                Ok(CtrlCommand::DIFFERTEST { path, length }) => {
                    #[cfg(feature = "differtest")]
                    {
                        self.differtest.init(&path);
                        self.differtest.ref_difftest_init(1234);
                        with_rwlock_read!(self.mem, mem, {
                            self.differtest.ref_difftest_memcpy(0x8000_0000, {
                                let mmt = mem.match_memory(MatchMsg::ADDR { addr: 0x8000_0000 });
                                let memory = match mmt {
                                    Ok(m) => m,
                                    Err(_) => panic!("Memory not found"),
                                };
                                memory.memory.as_ptr() as *mut std::ffi::c_void
                            }, length, differtest::DiffertestDirection::ToRef);
                        });
                        with_rwlock_read!(self.reg, reg, {
                            self.differtest.set_ref_reg(&reg);
                        });
                        self.resper.lock().unwrap().info("Differtest initialized");
                        self.result_sender.send(Ok(SimOk::DiffertestInit)).unwrap();
                    }

                    #[cfg(not(feature = "differtest"))]
                    {
                        self.resper.lock().unwrap().error("Differtest feature is not enabled");
                        self.result_sender.send(Err(SimErr::DiffertestFailedToInit)).unwrap();
                    }
                }

                Ok(CtrlCommand::FUNC { on_or_off, target }) => {
                    if on_or_off.is_none() {
                        self.func_print();
                        self.result_sender.send(Ok(SimOk::FunctionShow)).unwrap();
                        continue;
                    }

                    let on_or_off = on_or_off.unwrap();

                    if target.is_none() {
                        self.inst_trace = on_or_off;
                        self.inst_trace_buffer.0 = on_or_off;
                        self.func_print();
                        self.result_sender.send(Ok(SimOk::FunctionCtrl)).unwrap();
                        continue;
                    }

                    let target = target.unwrap();
                    
                    match target.as_str() {
                        "it" => {
                            self.inst_trace = on_or_off;
                            self.resper.lock().unwrap().option_log("Instruction trace", on_or_off);
                        }
                        "ir" => {
                            self.inst_trace_buffer.0 = on_or_off;
                            self.resper.lock().unwrap().option_log("Instruction trace buffer", on_or_off);
                        }
                        _ => {
                            self.resper.lock().unwrap().error("You should input valid target from [it, ir]");
                            self.result_sender.send(Err(SimErr::FuncInvalidTarget)).unwrap();
                            continue;
                        }
                    }

                    self.result_sender.send(Ok(SimOk::FunctionCtrl)).unwrap();
                }

                Ok(CtrlCommand::DEBUG { target }) => {
                    match target {
                        val if val == "ir" => {
                            for i in self.inst_trace_buffer.1.iter().rev() {
                                self.resper.lock().unwrap().trace(format!("{:?}", i).as_str());
                            }
                        }

                        val if val == "t" => {
                            self.resper.lock().unwrap().trace(format!("Execute times: {}", self.execte_times).as_str());
                        }

                        _ => {
                            self.resper.lock().unwrap().error("Invalid debug target");
                            self.result_sender.send(Err(SimErr::DebugInvalidTarget)).unwrap();
                            continue;
                        }
                    }
                    self.result_sender.send(Ok(SimOk::DebugTrace)).unwrap();
                }

                Err(RecvError) => {
                    self.resper.lock().unwrap().error("The command sender has been dropped, exiting...");
                    break;
                }

                _ => {
                    self.resper.lock().unwrap().error("Invalid command");
                    self.result_sender.send(Err(SimErr::InvalidCommand)).unwrap();
                }
            }
        }
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
