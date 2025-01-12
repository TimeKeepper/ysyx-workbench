use msg_resp as msgr;
use clap::Parser;

ysyx_macro::mod_pub!(monitor_parser, executer);

pub struct Monitor {
    pub name: String,

    pub msgr: msgr::Resper,
    pub cli_parser: monitor_parser::Cli,
    pub cmd_manager: monitor_parser::CommandManager,
}

impl Monitor {
    pub fn new(name: &str) -> Self {
        let cli_parser = monitor_parser::Cli::parse();

        let msgr = msgr::Resper::new(cli_parser.log.clone());
        let cmd_manager = monitor_parser::CommandManager::new(name);

        Self {
            name: name.to_string(),

            msgr,
            cli_parser,
            cmd_manager,
        }
    }

    pub fn init(&self) {
        if self.cli_parser.dut.is_some() {
            self.msgr.error("DUT file path is not implemented yet");
        }

        if self.cli_parser.elf.is_some() {
            self.msgr.error("ELF file path is not implemented yet");
        }
    }

    pub fn main_loop(&mut self) {
        loop {
            self.msgr.function_log("batch", self.cli_parser.batch);
            let cmd = self.cmd_manager.get_parser();
            match cmd {
                monitor_parser::Commands::Quit {} => break,
                _ => self.msgr.error("Command not implemented yet"),
            }
        }
    }
}
