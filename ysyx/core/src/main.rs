use msg_resp::{ResultMessage, CtrlCommand};
use std::sync::mpsc::{Sender, Receiver};
use std::sync::{mpsc, Arc, Mutex, RwLock};
use std::thread;

use state::{mmu::MMU, reg::RegisterBank, ProcessState};

fn main() {
    let mem = Arc::new(RwLock::new(MMU::new()));
    let reg = Arc::new(RwLock::new(RegisterBank::new()));
    let state = Arc::new(RwLock::new(ProcessState::STOP));

    let (cmd_sender, cmd_receiver) = mpsc::channel();  // B → A
    let (result_sender, result_receiver) = mpsc::channel(); // A → B

    let mut monitor = monitor::Monitor::new("nemu", cmd_sender, result_receiver, 
        mem.clone(), reg.clone(), state.clone());

    monitor.init();

    let handle = thread::spawn(move || {
        let mut simulator = simulator::nemu::Simulator::new(
            cmd_receiver, result_sender, 
            mem.clone(), reg.clone(), state.clone());

        simulator.run();
    });

    monitor.main_loop();

    handle.join().unwrap();
}
