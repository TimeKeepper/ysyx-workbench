use super::super::mmu::MMU;
use circular_queue::CircularQueue;
use msg_resp::SimOk;
use msg_resp::{CtrlCommand, ResultMessage};
use std::sync::mpsc::Receiver;
use std::sync::mpsc::Sender;
use std::thread;

pub struct Simulator {
    pub mmu: MMU,
    
    pub inst_trace_buffer: (bool, CircularQueue<u32>),
    pub inst_trace: bool,
    pub execte_times: (u32, u32),
    
    pub cmd_receiver: Receiver<CtrlCommand>,
    pub result_sender: Sender<ResultMessage>,
}

impl Simulator {
    pub fn new(cmd_receiver: Receiver<CtrlCommand>, result_sender: Sender<ResultMessage>) -> Self {
        let mut mmu = MMU::new();
        mmu.add_memory("sram",  0x0f00_0000, 0x0000_2000);
        mmu.add_memory("mrom",  0x2000_0000, 0x0000_1000);
        mmu.add_memory("flash", 0x3000_0000, 0x1000_0000);
        mmu.add_memory("psram", 0x8000_0000, 0x0800_0000);
        mmu.add_memory("sdram", 0xa000_0000, 0x0200_0000);

        Self {
            mmu,
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
