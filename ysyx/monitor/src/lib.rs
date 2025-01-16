use msg_resp as msgr;
use clap::Parser;
use owo_colors::OwoColorize;
use simulator::Simulator;

ysyx_macro::mod_pub!(monitor_parser, nemu, mmu, simulator, disassembler, differtest);

use simulator::SimulatorOk as simOk;
use simulator::SimulatorError as simErr;
use monitor_parser::Commands as Cmd;

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

        let msgr = msgr::Resper::new();
        let cmd_manager = monitor_parser::CommandManager::new(name);

        Self {
            name: name.to_string(),

            msgr,
            cli_parser,
            cmd_manager,
            sim: nemu::Simulator::new(),
        }
    }

    pub fn init(&mut self) {
        self.init_log();

        self.init_sim();

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
            
            let result = self.execute(cmd);

            if result == Ok(simOk::Quit) {
                break;
            }

            self.deal_result(result);
        }
    }

    fn init_log(&mut self) {
        if self.cli_parser.log {
            self.msgr.init();
        }
        self.msgr.function_log("log", self.cli_parser.log);
    }

    fn init_sim(&mut self) {
        if let Err(err) = self.sim.init(self.cli_parser.bin.clone()) {
            let error_message = match err {
                simErr::NoBinaryFile => "No binary file",
                simErr::BinaryFileNotFound => "Binary file not found",
                _ => "Unknown error",
            };
            self.msgr.error(error_message);
        } else {
            self.msgr.info("Binary file loaded");
        }
    }

    fn execute(&mut self, cmd: Cmd) -> Result<simOk, simErr> {
        match cmd {
            Cmd::Quit {} => {
                self.msgr.info("Quitting...");
                return Ok(simOk::Quit);
            },
            Cmd::Info { command } => {
                match command {
                    monitor_parser::InfoCommands::Register {target} => {
                        if target.is_none(){
                            for r in self.sim.executer.gpr.iter() {
                                self.msgr.trace(format!("{}: \t0x{:08x}", r.name.purple(), r.value.red()).as_str());
                            }
                            self.msgr.trace(format!("{}: \t0x{:08x}", self.sim.executer.pc.name.purple(), self.sim.executer.pc.value.red()).as_str());
                            return Ok(simOk::Nothing);
                        }
                        let target = target.unwrap();
                        for r in self.sim.executer.gpr.iter() {
                            if r.name == target {
                                self.msgr.trace(format!("{}: \t0x{:08x}", r.name.purple(), r.value.red()).as_str());
                                return Ok(simOk::Nothing);
                            }
                        }
                        if target == self.sim.executer.pc.name {
                            self.msgr.trace(format!("{}: \t0x{:08x}", self.sim.executer.pc.name.purple(), self.sim.executer.pc.value.red()).as_str());
                            return Ok(simOk::Nothing);
                        }
                        self.msgr.error("No matching register");
                        return Err(simErr::InvalidCommand);
                    }
                }
            },
            Cmd::Function { on_or_off, target } => {
                if target.is_none() {
                    self.msgr.error("No target specified");
                    return Err(simErr::InvalidCommand);
                }
                if on_or_off.is_none() {
                    self.msgr.error("No on/off specified");
                    return Err(simErr::InvalidCommand);
                }
                let on: bool = on_or_off.unwrap() == "on";
                let target: &str = &target.unwrap();
                let status = |feature: &str| format!("{} is {}", 
                    feature, 
                    if on { "on".green().to_string() } else { "off".red().to_string() }
                );

                match target {
                    "it" => {
                        self.sim.inst_trace = on;
                        self.msgr.info(&status("Instruction trace"));
                        return Ok(simOk::Nothing);
                    },
                    "ir" => {
                        self.sim.inst_trace_buffer.0 = on;
                        self.msgr.info(&status("Instruction trace buffer"));
                        return Ok(simOk::Nothing);
                    },
                    _ => {
                        self.msgr.error("You should input valid target from [it, ir]");
                        return Err(simErr::InvalidCommand);
                    },
                }
            },
            Cmd::SingleInstrcution { count } => {
                return self.sim.single_instruction(if count.is_some() { count.unwrap() } else { 1 });
            },
            _ => return Err(simErr::NotImplemented),
        }

    }

    fn deal_result(&mut self, result: Result<simOk, simErr>) {
        match result {
            Ok(_) => return,
            Err(err) => match err {
                simErr::NotImplemented => self.msgr.error("Not implemented yet"),
                simErr::InvalidCommand => self.msgr.error("Invalid command"),
                simErr::NoMatchingMemoryByAddress{addr} => self.msgr.error(format!("No matching memory {}", addr).as_str()),
                simErr::NoMatchingMemoryByName{name} => self.msgr.error(format!("No matching memory {}", name).as_str()),
                simErr::InstrctionDecodeFailed{inst} => self.msgr.error(format!("Instruction decode failed at PC 0x{:08x} with instruction 0x{:08x}", self.sim.executer.pc.value, inst).as_str()),
                simErr::UnknownInstruction{name} => self.msgr.error(format!("Unknown instruction {}", name.purple()).as_str()),
                _ => self.msgr.error("Unknown error"),
            },
        }
    }
}
