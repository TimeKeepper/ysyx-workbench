
use clap::Parser;
use msg_resp::CtrlCommand;
use msg_resp::MatchMsg;
use msg_resp as msgr;
use owo_colors::OwoColorize;
use state::reg::RegisterOps;
use state::mmu::{MMU, devices::{SerialFactory, TimerFactory}};
use state::reg::RegisterBank;
use state::ProcessState;

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

use std::sync::{Arc, Mutex};

pub struct Monitor {
    pub name: String,

    pub msgr: msgr::Resper,
    pub cli_parser:Cli,
    pub cmd_manager: CmM,

    // pub differtest: differtest::Differtest,
    // pub differtest_watchpoints: Vec<u32>,

    pub signal: Arc<AtomicBool>,
    
    pub cmd_sender: Sender<CtrlCommand>,
    pub result_receiver: Receiver<ResultMessage>,

    pub mem: Arc<Mutex<MMU>>,
    pub reg: Arc<Mutex<RegisterBank>>,
    pub state: Arc<Mutex<ProcessState>>,
}

impl Monitor {
    pub fn new(name: &str, cmd_sender: Sender<CtrlCommand>, result_receiver: Receiver<ResultMessage>, 
        mem: Arc<Mutex<state::mmu::MMU>>, 
        reg: Arc<Mutex<state::reg::RegisterBank>>,
        state: Arc<Mutex<ProcessState>>) -> Self {
        let cli_parser = monitor_parser::Cli::parse();

        let msgr = msgr::Resper::new();
        let cmd_manager = monitor_parser::CommandManager::new(name);

        let signal = Arc::new(AtomicBool::new(false));
        Self {
            name: name.to_string(),

            msgr,
            cli_parser,
            cmd_manager,
            // sim: Box::new(nemu::Simulator::new()),
            // differtest: differtest::Differtest::new(),
            // differtest_watchpoints: Vec::new(),

            signal,

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
            self.cmd_sender.send(CtrlCommand::FUNC { on_or_off: true, target: Some("it".to_string()) }).unwrap();
            self.cmd_sender.send(CtrlCommand::FUNC { on_or_off: true, target: Some("ir".to_string()) }).unwrap();
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

        loop {
            let state = self.state.lock().unwrap();
            if *state == ProcessState::QUIT || *state == ProcessState::ABORT {
                break;
            }
            drop(state); // Release the lock before executing commands

            let cmd = self.cmd_manager.get_parser();
            let result = self.execute(cmd);
            self.deal_result(result);
        }
    }

    fn init_mem(&mut self) {
        let mut mem = self.mem.lock().unwrap();
        
        mem.add_memory("sram",  0x0f00_0000, 0x0000_2000);
        mem.add_memory("mrom",  0x2000_0000, 0x0000_1000);
        mem.add_memory("flash", 0x3000_0000, 0x1000_0000);
        mem.add_memory("psram", 0x8000_0000, 0x0800_0000);
        mem.add_memory("sdram", 0xa000_0000, 0x0200_0000);

        mem.add_device(SerialFactory::new(0x1000_0000));
        mem.add_device(TimerFactory::new(0x1000_2000));

        drop(mem);
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
        self.reg.lock().unwrap().write_pc(0x80000000);

        if self.cli_parser.bin.is_none() {
            return Err(SimErr::NoBinaryFile);
        }
        let bin = std::fs::read(self.cli_parser.bin.clone().unwrap());
        if bin.is_err() {
            return Err(SimErr::BinaryFileNotFound);
        }
        let bin: Vec<u8> = bin.unwrap();

        self.mem.lock().unwrap().load("psram", &bin)?;

        if let Some(diffpath) = &self.cli_parser.dut {
            // self.differtest.init(&diffpath);
            // self.differtest.ref_difftest_init(1234);
            // self.differtest.ref_difftest_memcpy(
            //     0x8000_0000,
            //     {let mmt = self.mem
            //             .lock().unwrap()
            //             .match_memory(MatchMsg::ADDR { addr: 0x8000_0000})
            //             .ok()
            //             .unwrap();
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
        //     self.differtest.set_ref_reg(&self.reg.lock().unwrap());
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
        let mut state = self.state.lock().unwrap();
        match result {
            Ok(_) => return,
            Err(err) => match err {
                SimErr::Signal => {
                    *state = ProcessState::STOP;
                }
                SimErr::Ebreak { is_good } => {
                    *state = if is_good {
                        self.msgr.success("Hit Good TRAP");
                        ProcessState::DONE
                    } else {
                        self.msgr.error("Hit Bad TRAP");
                        ProcessState::TRAP
                    }
                },

                SimErr::NotImplemented => self.msgr.error("Not implemented yet"),
                SimErr::InvalidCommand => self.msgr.error("Invalid command"),
                SimErr::InvalidRegIndentifier => self.msgr.error("Invalid register identifier"),

                SimErr::BinaryFileNotFound => self.msgr.error("Binary file not found"),
                SimErr::NoBinaryFile => self.msgr.error("No binary file"),

                SimErr::DeviceCannotBeLoad { name } => {
                    self.msgr.error(format!("Device {} cannot be loaded", name).as_str())
                },

                SimErr::DiffertestFailed => {
                    self.msgr.error("Differtest failed");
                    *state = ProcessState::TRAP
                },
                SimErr::NoMatchingMemory { msg } => {
                    self
                    .msgr
                    .error(format!("No matching memory {}", msg).as_str());
                    *state = ProcessState::TRAP
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
                    *state = ProcessState::TRAP
                },
                SimErr::InstrctionExecuteFailed { name } => {
                    self
                    .msgr
                    .error(format!("Failed to execute instrcution {}", name.purple()).as_str());
                    *state = ProcessState::TRAP
                },
            },
        }
    }
}

impl Drop for Monitor {
    fn drop(&mut self) {
        let state = self.state.lock().unwrap();

        self.msgr.info("Exiting...");
        match *state {
            ProcessState::QUIT => self.msgr.success("Exited normally"),
            ProcessState::ABORT => self.msgr.error("Exited abnormally"),
            _ => self.msgr.error("Unknown exit status"),
        }

        drop(state);
    }
}
