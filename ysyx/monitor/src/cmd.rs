use simulator::Simulator;
use simulator::SimulatorError as simErr;
use simulator::SimulatorOk as simOk;
use std::os::raw::c_void;
use std::result::Result;

use owo_colors::OwoColorize;

use crate::differtest::DiffertestDirection;
use crate::differtest::Riscv32CpuState;
use crate::{
    monitor_parser::{self, InfoCommands},
    Monitor, MonitorState,
};

impl Monitor {
    pub fn cmd_q(&mut self) -> Result<simOk, simErr> {
        self.state = if self.state == MonitorState::TRAP {
            MonitorState::ABORT
        } else {
            MonitorState::QUIT
        };
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
        if self.state == MonitorState::TRAP {
            self.msgr.error("Monitor is in trap state");
            return Err(simErr::InvalidCommand);
        }

        let count = if count.is_none() { 1 } else { count.unwrap() };

        let mut orig = || -> Result<(), simErr> {
            self.sim.single_instruction()?;
            if self.sim.mmu.is_attch_device == false {
                self.differtest.ref_difftest_exec(1);
                self.differtest.difftest_step(&self.sim.cpu_state)?;
            } else {
                let mut regcpy = Riscv32CpuState {
                    gpr: {
                        let gpr_vec = self.sim.cpu_state.gpr.clone().into_iter().map(|gpr| gpr.value).collect::<Vec<u32>>();
                        let mut gpr_array = [0u32; 32];
                        gpr_array.copy_from_slice(&gpr_vec[..32]);
                        gpr_array
                    },
                    pc: self.sim.cpu_state.pc.value,
                };
                self.differtest.ref_difftest_regcpy(&mut regcpy as *mut _ as *mut c_void, DiffertestDirection::ToRef);
            }
            Ok(())
        };

        if count == 0 {
            loop {
                orig()?;
            }
        }

        for _ in 0..count {
            orig()?;
        }

        Ok(simOk::InstructionExecuted)
    }

    pub fn cmd_c(&mut self) -> Result<simOk, simErr> {
        self.cmd_si(Some(0))
    }

    pub fn cmd_ir(&mut self) -> Result<simOk, simErr> {
        self.sim.instruction_ring_buffer();
        Ok(simOk::Nothing)
    }

    pub fn cmd_t(&mut self) -> Result<simOk, simErr> {
        self.sim.times();
        Ok(simOk::Nothing)
    }
}
