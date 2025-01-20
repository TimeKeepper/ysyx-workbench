
use clap::Parser;
use msg_resp as msgr;
use owo_colors::OwoColorize;

use super::monitor_parser;
use super::monitor_parser::Commands as Cmd;
use super::monitor_parser::Cli as Cli;
use super::monitor_parser::CommandManager as CmM;

use super::differtest;

use std::os::raw::c_void;

use simulator::nemu;

use simulator::SimulatorError as simErr;
use simulator::SimulatorOk as simOk;
use simulator::mmu;

#[derive(Debug, PartialEq, Clone)]
pub enum MonitorState {
    RUNNING,
    TRAP,
    QUIT,
    ABORT,
}


pub struct Monitor {
    pub name: String,

    pub msgr: msgr::Resper,
    pub cli_parser:Cli,
    pub cmd_manager: CmM,

    pub sim: nemu::Simulator,
    pub differtest: differtest::Differtest,

    pub state: MonitorState,
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

            state: MonitorState::RUNNING,
        }
    }

    pub fn init(&mut self) {
        self.init_log();

        match self.init_sim() {
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
        while (self.state != MonitorState::QUIT) && (self.state != MonitorState::ABORT) {
            let cmd = self.cmd_manager.get_parser();

            let result = self.execute(cmd);

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
            self.differtest.ref_difftest_memcpy(
                0x8000_0000,
                self.sim
                    .mmu
                    .match_memory(mmu::MatchMsg::ADDR { addr: 0x8000_0000})
                    .ok()
                    .unwrap()
                    .get_memory()
                    .as_mut_ptr() as *mut c_void,
                bin.len() as u64,
                differtest::DiffertestDirection::ToRef,
            );
        }
        self.msgr
            .function_log("differtest", self.cli_parser.dut.is_some());
        self.differtest.set_ref_reg(&self.sim.cpu_state);

        Ok(())
    }

    fn execute(&mut self, cmd: Cmd) -> Result<simOk, simErr> {
        match cmd {
            Cmd::Quit {} => self.cmd_q(),
            Cmd::Info { command } => self.cmd_info(command),
            Cmd::Function { on_or_off, target } => self.cmd_func(on_or_off, target),
            Cmd::SingleInstrcution { count } => self.cmd_si(count),
            Cmd::Continue {  } => self.cmd_c(),
            _ => Err(simErr::NotImplemented),
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

                simErr::DiffertestFailed => {
                    self.msgr.error("Differtest failed");
                    self.state = MonitorState::TRAP
                },
                simErr::NoMatchingMemory { msg } => {
                    self
                    .msgr
                    .error(format!("No matching memory {}", msg).as_str());
                    self.state = MonitorState::TRAP
                },
                simErr::InstrctionDecodeFailed { inst } => {
                    self.msgr.error(
                    format!(
                        "Instruction decode failed at PC 0x{:08x} with instruction 0x{:08x}",
                            self.sim.cpu_state.pc.value, inst
                        )
                        .as_str(),
                    );
                    self.state = MonitorState::TRAP
                },
                simErr::InstrctionExecuteFailed { name } => {
                    self
                    .msgr
                    .error(format!("Failed to execute instrcution {}", name.purple()).as_str());
                    self.state = MonitorState::TRAP
                },
            },
        }
    }
}

impl Drop for Monitor {
    fn drop(&mut self) {
        self.msgr.info("Exiting...");
        match self.state {
            MonitorState::QUIT => self.msgr.success("Exited normally"),
            MonitorState::ABORT => self.msgr.error("Exited abnormally"),
            _ => self.msgr.error("Unknown exit status"),
        }
    }
}
