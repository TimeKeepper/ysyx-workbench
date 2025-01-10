use ysyx::commands as cmd;
use ysyx::msg_resp as msgr;

fn main() {
    let mut command_manager = cmd::CommandManager::new("nemu");

    loop {
        let cmd = command_manager.get_parser();
        match cmd {
            cmd::Commands::Quit {} => break,
            _ => println!("{}", msgr::respstring("This Command have not implement yet", msgr::RespType::Error)),
        }
    }
}
