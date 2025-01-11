use ysyx::commands as cmd;
use ysyx::msg_resp as msgr;

fn main() {
    let (batch, log, dut, elf) = ysyx::cli::parser();

    let msgr = msgr::Resper::new();

    if batch {
        println!("{}", msgr.error("Batch mode is not implemented yet"));
    }

    if log.is_some() {
        println!("{}", msgr.error("Log file path is not implemented yet"));
    }

    if dut.is_some() {
        println!("{}", msgr.error("DUT file path is not implemented yet"));
    }

    if elf.is_some() {
        println!("{}", msgr.error("ELF file path is not implemented yet"));
    }

    let mut command_manager = cmd::CommandManager::new("nemu");

    loop {
        let cmd = command_manager.get_parser();
        match cmd {
            cmd::Commands::Quit {} => break,
            _ => println!("{}", msgr.error("Command not implemented yet")),
        }
    }
}
