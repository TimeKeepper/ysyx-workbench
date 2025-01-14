use msg_resp as msgr;
use clap::Parser;
use owo_colors::OwoColorize;
use simulator::Simulator;

ysyx_macro::mod_pub!(monitor_parser, nemu, mmu, simulator);

use simulator::SimulatorError as simErr;

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
        if self.cli_parser.log {
            self.msgr.init();
        } else {
            self.msgr.function_log("log", false);
        }

        match self.sim.init(self.cli_parser.bin.clone()) {
            Ok(_) => self.msgr.info("Binary file loaded"),
            Err(err) => {
                match err {
                    simErr::NoBinaryFile => self.msgr.error("No binary file"),
                    simErr::BinaryFileNotFound => self.msgr.error("Binary file not found"),
                    _ => self.msgr.error("Unknown error"),
                }
            },
        }

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
            
            let result: Result<simulator::SimulatorOk, simErr>;

            match cmd {
                monitor_parser::Commands::Quit {} => break,
                monitor_parser::Commands::Info { command } => {
                    match command {
                        monitor_parser::InfoCommands::Register { } => {
                            for r in self.sim.executer.gpr.iter().enumerate() {
                                self.msgr.trace(format!("x{}: \t0x{:08x}", r.0, r.1).as_str());
                            }
                            self.msgr.trace(format!("PC: \t0x{:08x}", self.sim.executer.pc).as_str());
                        }
                    }
                    result = Ok(simulator::SimulatorOk::Nothing);
                }
                monitor_parser::Commands::Function { on_or_off, target } => {
                    if target.is_none() {
                        self.msgr.error("No target specified");
                        continue;
                    }
                    if on_or_off.is_none() {
                        self.msgr.error("No on/off specified");
                        continue;
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
                        },
                        "ir" => {
                            self.sim.inst_trace_buffer.0 = on;
                            self.msgr.info(&status("Instruction trace buffer"));
                        },
                        _ => self.msgr.error("Unknown target"),
                    }
                    result = Ok(simulator::SimulatorOk::Nothing);
                }
                monitor_parser::Commands::SingleInstrcution(time) => {
                    result = self.sim.single_instruction(if time.count.is_some() { time.count.unwrap() } else { 1 });
                }
                _ => result = Err(simErr::NotImplemented),
            }

            if result.is_ok() {
                continue;
            }
            let result = result.err().unwrap();

            match result {
                simErr::NotImplemented => self.msgr.error("Not implemented yet"),
                simErr::NoMatchingMemoryByAddress{addr} => self.msgr.error(format!("No matching memory {}", addr).as_str()),
                simErr::NoMatchingMemoryByName{name} => self.msgr.error(format!("No matching memory {}", name).as_str()),
                simErr::InstrctionDecodeFailed{inst} => self.msgr.error(format!("Instruction decode failed {:08x} : {:08x}", self.sim.executer.pc, inst).as_str()),
                simErr::UnknownInstruction{name} => self.msgr.error(format!("Unknown instruction {}", name.purple()).as_str()),

                _ => self.msgr.error("Unknown error"),
            }
        }
    }
}
