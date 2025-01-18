use std::result::Result;

use owo_colors::OwoColorize;

use crate::{
    monitor_parser::{self, InfoCommands},
    Monitor, MonitorState, Simulator,
};

use super::{simErr, simOk};

impl Monitor {
    pub fn cmd_q(&mut self) -> Result<simOk, simErr> {
        self.state = MonitorState::QUIT;
        Ok(simOk::Nothing)
    }

    pub fn cmd_info(&mut self, command: InfoCommands) -> Result<simOk, simErr> {
        match command {
            monitor_parser::InfoCommands::Register { target } => {
                if target.is_none() {
                    for r in self.sim.cpu_state.gpr.iter() {
                        self.msgr.trace(
                            format!("{}: \t0x{:08x}", r.name.purple(), r.value.red()).as_str(),
                        );
                    }
                    self.msgr.trace(
                        format!(
                            "{}: \t0x{:08x}",
                            self.sim.cpu_state.pc.name.purple(),
                            self.sim.cpu_state.pc.value.red()
                        )
                        .as_str(),
                    );
                    return Ok(simOk::Nothing);
                }
                let target = target.unwrap();
                for r in self.sim.cpu_state.gpr.iter() {
                    if r.name == target {
                        self.msgr.trace(
                            format!("{}: \t0x{:08x}", r.name.purple(), r.value.red()).as_str(),
                        );
                        return Ok(simOk::Nothing);
                    }
                }
                if target == self.sim.cpu_state.pc.name {
                    self.msgr.trace(
                        format!(
                            "{}: \t0x{:08x}",
                            self.sim.cpu_state.pc.name.purple(),
                            self.sim.cpu_state.pc.value.red()
                        )
                        .as_str(),
                    );
                    return Ok(simOk::Nothing);
                }
                self.msgr.error("No matching register");
                return Err(simErr::InvalidCommand);
            }
        }
    }

    pub fn cmd_func(
        &mut self,
        on_or_off: Option<String>,
        target: Option<String>,
    ) -> Result<simOk, simErr> {
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
        let status = |feature: &str| {
            format!(
                "{} is {}",
                feature,
                if on {
                    "on".green().to_string()
                } else {
                    "off".red().to_string()
                }
            )
        };

        match target {
            "it" => {
                self.sim.inst_trace = on;
                self.msgr.info(&status("Instruction trace"));
                return Ok(simOk::Nothing);
            }
            "ir" => {
                self.sim.inst_trace_buffer.0 = on;
                self.msgr.info(&status("Instruction trace buffer"));
                return Ok(simOk::Nothing);
            }
            _ => {
                self.msgr
                    .error("You should input valid target from [it, ir]");
                return Err(simErr::InvalidCommand);
            }
        }
    }

    pub fn cmd_si(&mut self, count: Option<u32>) -> Result<simOk, simErr> {
        for _ in 0..if count.is_none() { 1 } else { count.unwrap() } {
            self.sim.single_instruction()?;
            self.differtest.ref_difftest_exec(1);
            self.differtest.difftest_step(&self.sim.cpu_state)?;
        }
        Ok(simOk::InstructionExecuted)
    }
}
