use msg_resp::{CtrlCommand, Resper, ResultMessage};
use std::sync::mpsc::{Receiver, Sender};
use std::sync::{mpsc, Arc, Mutex, RwLock};
use std::thread;

use state::{mmu::MMU, reg::RegisterBank, ProcessState};

fn main() {
    let resper = Arc::new(Mutex::new(Resper::new()));

    let mem = Arc::new(RwLock::new(MMU::new(resper.clone())));
    let reg = Arc::new(RwLock::new(RegisterBank::new()));
    let state = Arc::new(RwLock::new(ProcessState::STOP));

    let (cmd_sender, cmd_receiver) = mpsc::channel(); // B → A
    let (result_sender, result_receiver) = mpsc::channel(); // A → B

    let mut monitor = monitor::Monitor::new(
        "nemu",
        cmd_sender,
        result_receiver,
        mem.clone(),
        reg.clone(),
        state.clone(),
        resper.clone(),
    );

    let handle = thread::spawn(move || {
        let mut simulator = simulator::Simulator::new(
            cmd_receiver,
            result_sender,
            mem.clone(),
            reg.clone(),
            state.clone(),
            resper.clone(),
        );
        
        simulator.run();
    });
    
    monitor.init();

    monitor.main_loop();

    handle.join().unwrap();
}
