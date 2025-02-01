
use clap::Parser;
use msg_resp::CtrlCommand;
use msg_resp::MatchMsg;
use msg_resp as msgr;
use owo_colors::OwoColorize;
use simulator::mmu::MMT;

use crate::monitor_parser::OperationMode;

use super::monitor_parser;
use super::monitor_parser::Commands as Cmd;
use super::monitor_parser::Cli as Cli;
use super::monitor_parser::CommandManager as CmM;

use super::differtest;

use std::os::raw::c_void;
use std::sync::atomic::AtomicBool;
use std::sync::mpsc::Receiver;
use std::sync::mpsc::Sender;

use simulator::nemu;

use msg_resp::{SimErr, ResultMessage};

#[derive(Debug, PartialEq, Clone)]
pub enum MonitorState {
    RUNNING,
    STOP,

    // Done state
    DONE,
    TRAP,

    // Quit state
    QUIT,
    ABORT,
}

pub struct Monitor {
    pub name: String,

    pub msgr: msgr::Resper,
    pub cli_parser:Cli,
    pub cmd_manager: CmM,

    pub differtest: differtest::Differtest,
    pub differtest_watchpoints: Vec<u32>,

    pub state: MonitorState,

    pub signal: std::sync::Arc<AtomicBool>,
    
    pub cmd_sender: Sender<CtrlCommand>,
    pub result_receiver: Receiver<ResultMessage>,

    pub mem: std::sync::Arc<std::sync::Mutex<state::mmu::MMU>>,
    pub reg: std::sync::Arc<std::sync::Mutex<state::reg::RegisterBank>>,
}

impl Monitor {
    pub fn new(name: &str, cmd_sender: Sender<CtrlCommand>, result_receiver: Receiver<ResultMessage>, 
        mem: std::sync::Arc<std::sync::Mutex<state::mmu::MMU>>, 
        reg: std::sync::Arc<std::sync::Mutex<state::reg::RegisterBank>>) -> Self {
        let cli_parser = monitor_parser::Cli::parse();

        let msgr = msgr::Resper::new();
        let cmd_manager = monitor_parser::CommandManager::new(name);

        let signal = std::sync::Arc::new(AtomicBool::new(false));
        Self {
            name: name.to_string(),

            msgr,
            cli_parser,
            cmd_manager,
            // sim: Box::new(nemu::Simulator::new()),
            differtest: differtest::Differtest::new(),
            differtest_watchpoints: Vec::new(),

            state: MonitorState::STOP,

            signal,

            cmd_sender,
            result_receiver,

            mem,
            reg,
        }
    }

    pub fn init(&mut self) {
        self.mem.lock().unwrap().add_memory("sram",  0x0f00_0000, 0x0000_2000);
        self.mem.lock().unwrap().add_memory("mrom",  0x2000_0000, 0x0000_1000);
        self.mem.lock().unwrap().add_memory("flash", 0x3000_0000, 0x1000_0000);
        self.mem.lock().unwrap().add_memory("psram", 0x8000_0000, 0x0800_0000);
        self.mem.lock().unwrap().add_memory("sdram", 0xa000_0000, 0x0200_0000);

        self.init_signal();

        self.init_log();

        match self.init_sim() {
            Ok(_) => self.msgr.info("Simulator initialized"),
            Err(err) => match err {
                SimErr::NoBinaryFile => self.msgr.error("No binary file"),
                SimErr::BinaryFileNotFound => self.msgr.error("Binary file not found"),
                _ => self.msgr.error("Unknown error"),
            },
        }

        if self.cli_parser.elf.is_some() {
            self.msgr.error("ELF file path is not implemented yet");
        }

        if self.cli_parser.debug {
            // _ = self.sim.func_ctrl(true, Some("it"));
            // _ = self.sim.func_ctrl(true, Some("ir"));
        }
    }

    pub fn main_loop(&mut self) {
        if self.cli_parser.batch {
            self.msgr.info("Execute in Batch mode");
            let result = self.cmd_c();
            self.deal_result(result);
            let _ = self.cmd_q();
            return;
        }

        while (self.state != MonitorState::QUIT) && (self.state != MonitorState::ABORT) {
            let cmd = self.cmd_manager.get_parser();

            let result = self.execute(cmd);

            self.deal_result(result);
        }
    }

    fn init_signal(&mut self) {
        let signal = self.signal.clone();
        ctrlc::set_handler(move || {
            println!("received Ctrl+C!");
            signal.store(true, std::sync::atomic::Ordering::SeqCst);
        })
        .expect("Error setting Ctrl-C handler");
    }

    fn init_log(&mut self) {
        if self.cli_parser.log {
            self.msgr.init();
        }
        self.msgr.option_log("log", self.cli_parser.log);
    }

    fn init_sim(&mut self) -> Result<(), SimErr> {
        if self.cli_parser.bin.is_none() {
            return Err(SimErr::NoBinaryFile);
        }
        let bin = std::fs::read(self.cli_parser.bin.clone().unwrap());
        if bin.is_err() {
            return Err(SimErr::BinaryFileNotFound);
        }
        let bin = bin.unwrap();

        // self.sim.get_mem_state().load("psram", &bin)?;

        if let Some(diffpath) = &self.cli_parser.dut {
            self.differtest.init(&diffpath);
            self.differtest.ref_difftest_init(1234);
            // self.differtest.ref_difftest_memcpy(
            //     0x8000_0000,
            //     {let mmt = self.sim
            //         .get_mem_state()
            //         .match_memory(MatchMsg::ADDR { addr: 0x8000_0000})
            //         .ok()
            //         .unwrap();
            //         let memory = match mmt {
            //             MMT::Memory(memory) => memory,
            //             _ => panic!("No memory"),
            //         };
            //         memory
            //         .memory
            //         .as_mut_ptr() 
            //         as *mut c_void
            //     },
            //     bin.len() as u64,
            //     differtest::DiffertestDirection::ToRef,
            // );
            // self.differtest.set_ref_reg(&self.sim.get_reg_state());
        }
        self.msgr
            .option_log("differtest", self.cli_parser.dut.is_some());

        Ok(())
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
                SimErr::Signal => {
                    self.state = MonitorState::STOP;
                }
                SimErr::Ebreak { is_good } => {
                    self.state = if is_good {
                        self.msgr.success("Hit Good TRAP");
                        MonitorState::DONE
                    } else {
                        self.msgr.error("Hit Bad TRAP");
                        MonitorState::TRAP
                    }
                },

                SimErr::NotImplemented => self.msgr.error("Not implemented yet"),
                SimErr::InvalidCommand => self.msgr.error("Invalid command"),

                SimErr::BinaryFileNotFound => self.msgr.error("Binary file not found"),
                SimErr::NoBinaryFile => self.msgr.error("No binary file"),

                SimErr::DeviceCannotBeLoad { name } => {
                    self.msgr.error(format!("Device {} cannot be loaded", name).as_str())
                },

                SimErr::DiffertestFailed => {
                    self.msgr.error("Differtest failed");
                    self.state = MonitorState::TRAP
                },
                SimErr::NoMatchingMemory { msg } => {
                    self
                    .msgr
                    .error(format!("No matching memory {}", msg).as_str());
                    self.state = MonitorState::TRAP
                },
                SimErr::InstrctionDecodeFailed { inst } => {
                    // self.msgr.error(
                    // format!(
                    //     "Instruction decode failed at PC 0x{:08x} with instruction 0x{:08x}",
                    //         // self.sim.get_reg_state().pc, 
                    //         inst
                    //     )
                    //     .as_str(),
                    // );
                    self.state = MonitorState::TRAP
                },
                SimErr::InstrctionExecuteFailed { name } => {
                    self
                    .msgr
                    .error(format!("Failed to execute instrcution {}", name.purple()).as_str());
                    self.state = MonitorState::TRAP
                },
            },
        }
    }
}

impl Drop for Monitor {
    fn drop(&mut self) {
        self.msgr.info("Exiting...");
        match self.state {
            MonitorState::QUIT => self.msgr.success("Exited normally"),
            MonitorState::ABORT => self.msgr.error("Exited abnormally"),
            _ => self.msgr.error("Unknown exit status"),
        }
    }
}
