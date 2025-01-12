use crate::commands as cmd;
use crate::msg_resp as msgr;
use crate::cli as cli;
use clap::Parser;

pub struct Monitor {
    pub name: String,

    pub msgr: msgr::Resper,
    pub cli_parser: cli::Cli,
    pub cmd_manager: cmd::CommandManager,
}

impl Monitor {
    pub fn new(name: &str) -> Self {
        let cli_parser = cli::Cli::parse();

        let msgr = msgr::Resper::new(cli_parser.log.clone());
        let cmd_manager = cmd::CommandManager::new(name);

        Self {
            name: name.to_string(),

            msgr,
            cli_parser,
            cmd_manager,
        }
    }

    pub fn init(&self) {
        if self.cli_parser.batch {
            self.msgr.error("Batch mode is not implemented yet");
        }

        if self.cli_parser.dut.is_some() {
            self.msgr.error("DUT file path is not implemented yet");
        }

        if self.cli_parser.elf.is_some() {
            self.msgr.error("ELF file path is not implemented yet");
        }
    }

    pub fn main_loop(&mut self) {
        loop {
            let cmd = self.cmd_manager.get_parser();
            match cmd {
                cmd::Commands::Quit {} => break,
                _ => self.msgr.error("Command not implemented yet"),
            }
        }
    }
}
