
use clap::Parser;
use msg_resp::CtrlCommand;
use msg_resp::MatchMsg;
use msg_resp as msgr;
use owo_colors::OwoColorize;
use state::reg::RegisterOps;
use state::mmu::{MMU, devices::{SerialFactory, TimerFactory}};
use state::reg::RegisterBank;
use state::ProcessState;
use ysyx_macro::with_rwlock_write;

use crate::monitor_parser::OperationMode;

use super::monitor_parser;
use super::monitor_parser::Commands as Cmd;
use super::monitor_parser::Cli as Cli;
use super::monitor_parser::CommandManager as CmM;

use std::os::raw::c_void;
use std::sync::atomic::AtomicBool;
use std::sync::mpsc::Receiver;
use std::sync::mpsc::Sender;

use simulator::nemu;

use msg_resp::{SimErr, ResultMessage};

use std::sync::{Arc, Mutex, RwLock};

pub struct Monitor {
    pub name: String,

    pub resper: Arc<Mutex<msgr::Resper>>,

    pub cli_parser:Cli,
    pub cmd_manager: CmM,
    
    pub cmd_sender: Sender<CtrlCommand>,
    pub result_receiver: Receiver<ResultMessage>,

    pub mem: Arc<RwLock<MMU>>,
    pub reg: Arc<RwLock<RegisterBank>>,
    pub state: Arc<RwLock<ProcessState>>,
}

impl Monitor {
    pub fn new(name: &str, cmd_sender: Sender<CtrlCommand>, result_receiver: Receiver<ResultMessage>, 
        mem: Arc<RwLock<state::mmu::MMU>>, 
        reg: Arc<RwLock<state::reg::RegisterBank>>,
        state: Arc<RwLock<ProcessState>>,
        resper: Arc<Mutex<msgr::Resper>>) -> Self {
        let cli_parser = monitor_parser::Cli::parse();

        let cmd_manager = monitor_parser::CommandManager::new(name);

        Self {
            name: name.to_string(),

            resper,
            cli_parser,
            cmd_manager,

            cmd_sender,
            result_receiver,

            mem,
            reg,
            state,
        }
    }

    pub fn init(&mut self) {
        self.init_mem();

        self.init_signal();

        self.init_log();

        self.init_sim();

        if self.cli_parser.elf.is_some() {
            self.resper.lock().unwrap().error("ELF file path is not implemented yet");
        }

        if self.cli_parser.debug {
            self.cmd_sender.send(CtrlCommand::FUNC { on_or_off: true, target: Some("it".to_string()) }).unwrap();
            self.cmd_sender.send(CtrlCommand::FUNC { on_or_off: true, target: Some("ir".to_string()) }).unwrap();
        }
    }

    pub fn main_loop(&mut self) {
        if self.cli_parser.batch {
            self.resper.lock().unwrap().info("Execute in Batch mode");
            let result = self.cmd_c();
            self.deal_result(result);
            let _ = self.cmd_q();
            return;
        }

        loop {
            if *self.state.read().unwrap() == ProcessState::QUIT || *self.state.read().unwrap() == ProcessState::ABORT {
                break;
            }

            let cmd = self.cmd_manager.get_parser();
            let result = self.execute(cmd);
            self.deal_result(result);
        }
    }

    fn init_mem(&mut self) {
        with_rwlock_write!(self.mem, mem, {
            mem.add_memory("sram",  0x0f00_0000, 0x0000_2000);
            mem.add_memory("mrom",  0x2000_0000, 0x0000_1000);
            mem.add_memory("flash", 0x3000_0000, 0x1000_0000);
            mem.add_memory("psram", 0x8000_0000, 0x0800_0000);
            mem.add_memory("sdram", 0xa000_0000, 0x0200_0000);

            mem.add_device(SerialFactory::new(0x1000_0000));
            mem.add_device(TimerFactory::new(0x1000_2000));
        });
    }

    fn init_signal(&mut self) {
        let resper = self.resper.clone();
        let state = self.state.clone();
        ctrlc::set_handler(move || {
            resper.lock().unwrap().info("Ctrl-C received");
            *state.write().unwrap() = ProcessState::QUIT;
        })
        .expect("Error setting Ctrl-C handler");
    }

    fn init_log(&mut self) {
        if self.cli_parser.log {
            self.resper.lock().unwrap().init();
        }
        self.resper.lock().unwrap().option_log("log", self.cli_parser.log);
    }

    fn init_sim(&mut self) {
        with_rwlock_write!(self.reg, reg, {
            reg.write_pc(0x8000_0000);
        });

        if self.cli_parser.bin.is_none() {
            self.resper.lock().unwrap().error("No binary file");
            return;
        }
        let bin = std::fs::read(self.cli_parser.bin.clone().unwrap());
        if bin.is_err() {
            self.resper.lock().unwrap().error("Binary file not found");
            *self.state.write().unwrap() = ProcessState::ABORT;
            return;
        }
        let bin: Vec<u8> = bin.unwrap();

        with_rwlock_write!(self.mem, mem, {
            mem.load("psram", &bin);
        });

        if let Some(diffpath) = &self.cli_parser.dut {
            self.cmd_sender.send(CtrlCommand::DIFFERTEST { path: diffpath.clone(), length: bin.len() as u64 }).unwrap();
        }
    }

    fn execute(&mut self, cmd: Cmd) -> ResultMessage {
        match cmd {
            Cmd::Quit {} => self.cmd_q(),

            Cmd::Info { target, index } => self.cmd_info(target, index),

            Cmd::Examine { addr, length } => self.cmd_x(addr, length),
            Cmd::MemoryMap {  } => self.cmd_mm(),
            Cmd::MemoryDiffertestWatchpoint { addr } => self.cmd_mdw(addr),

            Cmd::Times {  } => self.cmd_t(),

            Cmd::Function { on_or_off, target } => {
                let on_or_off = match on_or_off {
                    Some(OperationMode::On) => true,
                    _ => false,
                };
                self.cmd_func(on_or_off, target)
            },

            Cmd::SingleInstrcution { count } => self.cmd_si(count),
            Cmd::InstructionRingBuffer {  } => self.cmd_ir(),
            
            Cmd::Continue {  } => self.cmd_c(),
        }
    }

    fn deal_result(&mut self, result: ResultMessage) {
        match result {
            Ok(_) => return,
            Err(err) => match err {
                SimErr::Ebreak { is_good } => {
                    with_rwlock_write!(self.state, state, {
                        *state = if is_good {
                            self.resper.lock().unwrap().success("Hit Good TRAP");
                            ProcessState::DONE
                        } else {
                            self.resper.lock().unwrap().error("Hit Bad TRAP");
                            ProcessState::TRAP
                        }
                    });
                },

                SimErr::NotImplemented => self.resper.lock().unwrap().error("Not implemented yet"),
                SimErr::InvalidCommand => self.resper.lock().unwrap().error("Invalid command"),
                SimErr::InvalidRegIndentifier => self.resper.lock().unwrap().error("Invalid register identifier"),

                SimErr::DiffertestFailed => {
                    self.resper.lock().unwrap().error("Differtest failed");
                    with_rwlock_write!(self.state, state, {
                        *state = ProcessState::TRAP
                    });
                },
                SimErr::NoMatchingMemory {} => {
                    with_rwlock_write!(self.state, state, {
                        *state = ProcessState::TRAP
                    });
                },
                SimErr::NoMatchingDevice { } => {
                    with_rwlock_write!(self.state, state, {
                        *state = ProcessState::TRAP
                    });
                },
                SimErr::InstrctionDecodeFailed { } => {
                    with_rwlock_write!(self.state, state, {
                        *state = ProcessState::TRAP
                    });
                },
                SimErr::InstrctionExecuteFailed { } => {
                    self
                    .resper.lock().unwrap()
                    .error(format!("Failed to execute instrcution").as_str());
                    with_rwlock_write!(self.state, state, {
                        *state = ProcessState::TRAP
                    });
                },
            },
        }
    }
}

impl Drop for Monitor {
    fn drop(&mut self) {
        let state = self.state.read().unwrap();

        self.resper.lock().unwrap().info("Exiting...");
        match *state {
            ProcessState::QUIT => self.resper.lock().unwrap().success("Exited normally"),
            ProcessState::ABORT => self.resper.lock().unwrap().error("Exited abnormally"),
            _ => self.resper.lock().unwrap().error("Unknown exit status"),
        }
    }
}
