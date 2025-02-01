use std::sync::mpsc;

fn main() {
    let (cmd_sender, cmd_receiver) = mpsc::channel();  // B → A
    let (result_sender, result_receiver) = mpsc::channel(); // A → B

    let mut monitor = monitor::Monitor::new("nemu", cmd_sender, result_receiver);

    monitor.init();

    monitor.main_loop();
}
