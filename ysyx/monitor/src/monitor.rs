use clap::Parser;
use msg_resp::CtrlCommand;
use msg_resp::{self as msgr};
use state::mmu::MMU;
use state::reg::RegisterBank;
use state::ProcessState;
use ysyx_macro::with_rwlock_write;

use super::monitor_parser;
use super::monitor_parser::Cli;
use super::monitor_parser::CommandManager as CmM;
use super::monitor_parser::Commands as Cmd;

use std::sync::mpsc::Receiver;
use std::sync::mpsc::Sender;

use msg_resp::{ResultMessage, SimErr};

use std::sync::{Arc, Mutex, RwLock};

pub struct Monitor {
    pub name: String,

    pub resper: Arc<Mutex<msgr::Resper>>,

    pub cli_parser: Cli,
    pub cmd_manager: CmM,

    pub cmd_sender: Sender<CtrlCommand>,
    pub result_receiver: Receiver<ResultMessage>,

    pub mem: Arc<RwLock<MMU>>,
    pub reg: Arc<RwLock<RegisterBank>>,
    pub state: Arc<RwLock<ProcessState>>,
}

impl Monitor {
    pub fn new(
        name: &str,
        cmd_sender: Sender<CtrlCommand>,
        result_receiver: Receiver<ResultMessage>,
        mem: Arc<RwLock<state::mmu::MMU>>,
        reg: Arc<RwLock<state::reg::RegisterBank>>,
        state: Arc<RwLock<ProcessState>>,
        resper: Arc<Mutex<msgr::Resper>>,
    ) -> Self {
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

    pub fn main_loop(&mut self) {
        if self.cli_parser.batch {
            self.resper.lock().unwrap().info("Execute in Batch mode");
            let result = self.cmd_c();
            self.deal_result(result);
            let _ = self.cmd_q();
            return;
        }

        loop {
            if matches!(
                *self.state.read().unwrap(),
                ProcessState::QUIT | ProcessState::ABORT
            ) {
                break;
            }

            let cmd = self.cmd_manager.get_parser();
            let result = self.execute(cmd);
            self.deal_result(result);
        }
    }

    fn execute(&mut self, cmd: Cmd) -> ResultMessage {
        match cmd {
            Cmd::Quit {} => self.cmd_q(),

            Cmd::Receive {} => self.cmd_r(),

            Cmd::Info { target, index } => self.cmd_info(target, index),

            Cmd::Examine { addr, length } => self.cmd_x(addr, length),
            Cmd::MemoryMap {} => self.cmd_mm(),
            Cmd::MemoryDiffertestWatchpoint { addr } => self.cmd_mdw(addr),

            Cmd::Times {} => self.cmd_t(),

            Cmd::Function { on_or_off, target } => self.cmd_func(on_or_off, target),

            Cmd::SingleInstrcution { count } => self.cmd_si(count),
            Cmd::InstructionRingBuffer {} => self.cmd_ir(),

            Cmd::Continue {} => self.cmd_c(),
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
                }

                SimErr::NotImplemented => self.resper.lock().unwrap().error("Not implemented yet"),
                SimErr::InvalidCommand => self.resper.lock().unwrap().error("Invalid command"),
                SimErr::InvalidRegIndentifier => self
                    .resper
                    .lock()
                    .unwrap()
                    .error("Invalid register identifier"),

                SimErr::DiffertestFailed => {
                    self.resper.lock().unwrap().error("Differtest failed");
                    with_rwlock_write!(self.state, state, { *state = ProcessState::TRAP });
                }
                SimErr::NoMatchingMemory {} => {
                    with_rwlock_write!(self.state, state, { *state = ProcessState::TRAP });
                }
                SimErr::NoMatchingDevice {} => {
                    with_rwlock_write!(self.state, state, { *state = ProcessState::TRAP });
                }
                SimErr::InstrctionDecodeFailed {} => {
                    with_rwlock_write!(self.state, state, { *state = ProcessState::TRAP });
                }
                SimErr::InstrctionExecuteFailed {} => {
                    self.resper
                        .lock()
                        .unwrap()
                        .error(format!("Failed to execute instrcution").as_str());
                    with_rwlock_write!(self.state, state, { *state = ProcessState::TRAP });
                }

                _ => (),
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
