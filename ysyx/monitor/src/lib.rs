use msg_resp as msgr;
use clap::Parser;
use owo_colors::OwoColorize;
use simulator::Simulator;

ysyx_macro::mod_pub!(monitor_parser, nemu, mmu, simulator, disassembler, differtest);

use simulator::SimulatorOk as simOk;
use simulator::SimulatorError as simErr;
use monitor_parser::Commands as Cmd;

use std::os::raw::c_void;

pub struct Monitor {
    pub name: String,

    pub msgr: msgr::Resper,
    pub cli_parser: monitor_parser::Cli,
    pub cmd_manager: monitor_parser::CommandManager,

    pub sim: nemu::Simulator,
    pub differtest: differtest::Differtest,
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
            differtest: differtest::Differtest::new(),
        }
    }

    pub fn init(&mut self) {
        self.init_log();

        match self.init_sim(){
            Ok(_) => self.msgr.info("Simulator initialized"),
            Err(err) => match err {
                simErr::NoBinaryFile => self.msgr.error("No binary file"),
                simErr::BinaryFileNotFound => self.msgr.error("Binary file not found"),
                _ => self.msgr.error("Unknown error"),
            },
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

    fn init_sim(&mut self) -> Result<(), simErr> {
        if self.cli_parser.bin.is_none() {
            return Err(simErr::NoBinaryFile);
        }
        let bin = std::fs::read(self.cli_parser.bin.clone().unwrap());
        if bin.is_err() {
            return Err(simErr::BinaryFileNotFound);
        }
        let bin = bin.unwrap();

        self.sim.mmu.load("psram", &bin)?;

        if let Some(diffpath) = &self.cli_parser.dut {
            self.differtest.init(&diffpath);
            self.differtest.ref_difftest_init(1234);
            self.differtest.ref_difftest_memcpy(0x8000_0000, self.sim.mmu.match_memory_by_addr(0x8000_0000).ok().unwrap().memory.as_mut_ptr() as *mut c_void, bin.len() as u64, differtest::DiffertestDirection::ToRef);
        }
        self.msgr.function_log("differtest", self.cli_parser.dut.is_some());
        self.differtest.set_ref_reg(&self.sim.cpu_state);

        Ok(())
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
                            for r in self.sim.cpu_state.gpr.iter() {
                                self.msgr.trace(format!("{}: \t0x{:08x}", r.name.purple(), r.value.red()).as_str());
                            }
                            self.msgr.trace(format!("{}: \t0x{:08x}", self.sim.cpu_state.pc.name.purple(), self.sim.cpu_state.pc.value.red()).as_str());
                            return Ok(simOk::Nothing);
                        }
                        let target = target.unwrap();
                        for r in self.sim.cpu_state.gpr.iter() {
                            if r.name == target {
                                self.msgr.trace(format!("{}: \t0x{:08x}", r.name.purple(), r.value.red()).as_str());
                                return Ok(simOk::Nothing);
                            }
                        }
                        if target == self.sim.cpu_state.pc.name {
                            self.msgr.trace(format!("{}: \t0x{:08x}", self.sim.cpu_state.pc.name.purple(), self.sim.cpu_state.pc.value.red()).as_str());
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
                for _ in 0..if count.is_none() { 1 } else { count.unwrap() } {
                    self.sim.single_instruction()?;
                    self.differtest.ref_difftest_exec(1);
                    self.differtest.difftest_step(&self.sim.cpu_state)?;
                }
                return Ok(simOk::InstructionExecuted);
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
                simErr::BinaryFileNotFound => self.msgr.error("Binary file not found"),
                simErr::NoBinaryFile => self.msgr.error("No binary file"),
                simErr::DiffertestFailed => self.msgr.error("Differtest failed"),
                simErr::NoMatchingMemoryByAddress{addr} => self.msgr.error(format!("No matching memory {}", addr).as_str()),
                simErr::NoMatchingMemoryByName{name} => self.msgr.error(format!("No matching memory {}", name).as_str()),
                simErr::InstrctionDecodeFailed{inst} => self.msgr.error(format!("Instruction decode failed at PC 0x{:08x} with instruction 0x{:08x}", self.sim.cpu_state.pc.value, inst).as_str()),
                simErr::UnknownInstruction{name} => self.msgr.error(format!("Unknown instruction {}", name.purple()).as_str()),
            },
        }
    }
}
