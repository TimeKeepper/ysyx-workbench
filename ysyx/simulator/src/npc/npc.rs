use circular_queue::CircularQueue;
use msg_resp::SimOk;
use msg_resp::{CtrlCommand, ResultMessage};
use std::sync::mpsc::Receiver;
use std::sync::mpsc::Sender;
use std::thread;

pub struct Simulator {
    // pub mmu: MMU,
    
    pub inst_trace_buffer: (bool, CircularQueue<u32>),
    pub inst_trace: bool,
    pub execte_times: (u32, u32),
    
    pub cmd_receiver: Receiver<CtrlCommand>,
    pub result_sender: Sender<ResultMessage>,
}

impl Simulator {
    pub fn new(cmd_receiver: Receiver<CtrlCommand>, result_sender: Sender<ResultMessage>) -> Self {

        Self {
            // mmu,
            inst_trace_buffer: (false, CircularQueue::with_capacity(10)),
            inst_trace: false,
            execte_times: (0, 0),

            cmd_receiver,
            result_sender,
        }
    }

    pub fn run(self) -> thread::JoinHandle<()> {
        let receiver = self.cmd_receiver;
        let sender = self.result_sender;
        thread::spawn(move || {
            loop {
                match receiver.recv() {
                    Ok(CtrlCommand::QUIT) => {
                        sender.send(Ok(SimOk::Nothing)).unwrap();
                        break;
                    }
                    _ => {}
                }
            }
        })
    }
}
