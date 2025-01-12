use ysyx::monitor;

fn main() {
    let mut monitor = monitor::Monitor::new("nemu");

    monitor.init();

    monitor.main_loop();
}
