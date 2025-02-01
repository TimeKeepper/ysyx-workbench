use msg_resp::{ResultMessage, CtrlCommand};
use std::sync::mpsc::{Sender, Receiver};
use std::sync::mpsc;

fn main() {
    let (cmd_sender, cmd_receiver) = mpsc::channel();  // B → A
    let (result_sender, result_receiver) = mpsc::channel(); // A → B

    let mut monitor = monitor::Monitor::new("nemu", cmd_sender, result_receiver);

    let simulator = simulator::npc::Simulator::new(cmd_receiver, result_sender);

    monitor.init();

    let handle = simulator.run();

    monitor.main_loop();

    handle.join().unwrap();
}
