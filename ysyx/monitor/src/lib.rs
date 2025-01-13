use msg_resp as msgr;
use clap::Parser;
use simulator::Simulator;

ysyx_macro::mod_pub!(monitor_parser, nemu, mmu, simulator);

pub struct Monitor {
    pub name: String,

    pub msgr: msgr::Resper,
    pub cli_parser: monitor_parser::Cli,
    pub cmd_manager: monitor_parser::CommandManager,

    pub sim: nemu::Simulator,
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
            sim: nemu::Simulator::new(),
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
        self.msgr.function_log("batch", self.cli_parser.batch);
        loop {
            let cmd = self.cmd_manager.get_parser();
            
            let result: Result<String, simulator::SimulatorError>;
            
            match cmd {
                monitor_parser::Commands::Quit {} => break,
                monitor_parser::Commands::SingleInstrcution(time) => {
                    result = self.sim.single_instruction(if time.count.is_some() { time.count.unwrap() } else { 1 });
                }
                _ => result = Err(simulator::SimulatorError::NotImplemented),
            }

            if result.is_ok() {
                self.msgr.info(&result.unwrap());
                continue;
            }
            let result = result.err().unwrap();

            match result {
                simulator::SimulatorError::NotImplemented => self.msgr.error("Not implemented yet"),
                simulator::SimulatorError::NoMatchingMemory => self.msgr.error("No matching memory"),
                simulator::SimulatorError::InstrctionDecodeFailed => self.msgr.error("Instruction decode failed"),
                _ => self.msgr.error("Unknown error"),
            }
        }
    }
}
