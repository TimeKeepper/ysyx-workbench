use msg_resp::{SimOk, SimErr};
use msg_resp::{CtrlCommand, ResultMessage};
use msg_resp as msgr;
use std::sync::mpsc::Receiver;
use std::sync::mpsc::Sender;
use std::sync::{Arc, Mutex, RwLock};

use state::mmu::MMU;
use state::reg::RegisterBank;
use state::ProcessState;

pub struct Simulator {
    pub resper: Arc<Mutex<msgr::Resper>>,

    pub cmd_receiver: Receiver<CtrlCommand>,
    pub result_sender: Sender<ResultMessage>,

    pub mem: Arc<RwLock<MMU>>,
    pub reg: Arc<RwLock<RegisterBank>>,
    pub state: Arc<RwLock<ProcessState>>,
}

impl Simulator {
    pub fn new(cmd_receiver: Receiver<CtrlCommand>, result_sender: Sender<ResultMessage>, 
        mem: Arc<RwLock<state::mmu::MMU>>, 
        reg: Arc<RwLock<state::reg::RegisterBank>>,
        state: Arc<RwLock<ProcessState>>,
        resper: Arc<Mutex<msgr::Resper>>) -> Self {

        Self {
            resper,

            cmd_receiver,
            result_sender,

            mem,
            reg,
            state,
        }
    }

    pub fn run(self) {
        loop {
            let result = self.cmd_receiver.recv();

            match result {
                Ok(CtrlCommand::QUIT) => {
                    self.result_sender.send(Ok(SimOk::Quit)).unwrap();
                    break;
                }

                Ok(CtrlCommand::SI { count }) => {
                    
                }

                _ => {
                    self.resper.lock().unwrap().error("Invalid command");
                    self.result_sender.send(Err(SimErr::InvalidCommand)).unwrap();
                }
            }
        }
    }
}
