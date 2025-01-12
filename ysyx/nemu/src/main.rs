ysyx_macro::mod_flat!(elf_parser, rv_inst_parser);

fn main() {
    let mut monitor = monitor::Monitor::new("nemu");

    monitor.init();

    monitor.main_loop();
}
