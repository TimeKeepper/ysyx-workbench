use clap::Parser;
use msg_resp::CtrlCommand;
use msg_resp::{self as msgr};
use state::mmu::MMU;
use state::reg::RegisterBank;
use state::ProcessState;

use super::monitor_parser;
use super::monitor_parser::Cli;
use super::monitor_parser::CommandManager as CmM;

use std::sync::mpsc::Receiver;
use std::sync::mpsc::Sender;

use msg_resp::ResultMessage;

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
            self.batch();
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
            self.execute(cmd);
            // self.deal_result(result);
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
