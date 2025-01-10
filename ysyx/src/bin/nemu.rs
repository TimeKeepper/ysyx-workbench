use ysyx::commands as cmd;
use ysyx::msg_resp as msgr;

fn main() {
    let (batch, log, dut, elf) = ysyx::cli::parser();
    if batch {
        println!("{}", msgr::respstring("Batch mode is not implemented yet", msgr::RespType::Error));
        return;
    }

    if log.is_some() {
        println!("{}", msgr::respstring("Log file path is not implemented yet", msgr::RespType::Error));
        return;
    }

    if dut.is_some() {
        println!("{}", msgr::respstring("Differtest dut file is not implemented yet", msgr::RespType::Error));
        return;
    }

    if elf.is_some() {
        println!("{}", msgr::respstring("ELF file path is not implemented yet", msgr::RespType::Error));
        return;
    }

    let mut command_manager = cmd::CommandManager::new("nemu");

    loop {
        let cmd = command_manager.get_parser();
        match cmd {
            cmd::Commands::Quit {} => break,
            _ => println!("{}", msgr::respstring("This Command have not implement yet", msgr::RespType::Error)),
        }
    }
}
