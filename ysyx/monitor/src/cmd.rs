use simulator::mmu::Mask;
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

                if let Ok(i) = target.parse::<u32>() {
                    if (0..32).contains(&i) {
                        self.msgr.trace(
                            format!(
                                "{}: \t0x{:08x}",
                                self.sim.cpu_state.gpr[i as usize].name.purple(),
                                self.sim.cpu_state.gpr[i as usize].value.red()
                            )
                            .as_str(),
                        );
                        return Ok(simOk::Nothing);
                    }
                }

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
        on_or_off: bool,
        target: Option<String>,
    ) -> Result<simOk, simErr> {
        if target.is_none() {
            self.msgr.function_log("instruction trace", self.sim.inst_trace);
            self.msgr.function_log("instruction trace buffer", self.sim.inst_trace_buffer.0);
            return Ok(simOk::Nothing);
        }
        
        let target: &str = &target.unwrap();
        let status = |feature: &str| {
            format!(
                "{} is {}",
                feature,
                if on_or_off {
                    "on".green().to_string()
                } else {
                    "off".red().to_string()
                }
            )
        };

        match target {
            "it" => {
                self.sim.inst_trace = on_or_off;
                self.msgr.info(&status("Instruction trace"));
                return Ok(simOk::Nothing);
            }
            "ir" => {
                self.sim.inst_trace_buffer.0 = on_or_off;
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

        let mut orig = |trace: bool| -> Result<(), simErr> {
            self.sim.single_instruction(trace)?;
            if self.cli_parser.dut.is_none() {
                return Ok(());
            }
            if self.sim.mmu.is_attch_device == false {
                self.difftest_step()?;
            } else {
                let mut regcpy = Riscv32CpuState {
                    gpr: {
                        let gpr_vec = self.sim.cpu_state.gpr.clone().into_iter().map(|gpr| gpr.value).collect::<Vec<u32>>();
                        let mut gpr_array = [0u32; 32];
                        gpr_array.copy_from_slice(&gpr_vec[..32]);
                        gpr_array
                    },
                    pc: self.sim.cpu_state.pc.value,
                    csr: [0; 4096],
                };
                self.differtest.ref_difftest_regcpy(&mut regcpy as *mut _ as *mut c_void, DiffertestDirection::ToRef);
            }
            Ok(())
        };

        if count == 0 {
            loop {
                orig(false)?;
            }
        }

        for _ in 0..count {
            orig(count < 10)?;
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

    pub fn cmd_x(&mut self, addr: u32, length: Option<u32>) -> Result<simOk, simErr> {
        let mut addr = addr;
        let length = if length.is_none() { 1 } else { length.unwrap() };
        for _ in 0..length {
            addr = addr + 4;
            let data = self.sim.mmu.read(addr, Mask::None)?;
            self.msgr.trace(format!(" 0x{:08x}: \t0x{:08x}", addr.green(), data.red()).as_str());
        }
        
        Ok(simOk::Nothing)
    }

    pub fn cmd_mm(&mut self) -> Result<simOk, simErr> {
        for (name, range) in self.sim.mmu.memory_map() {
            self.msgr.trace(format!("{}: \t0x{:08x} - 0x{:08x}", name.red(), range.start.green(), range.end.green()).as_str());
        }
        Ok(simOk::Nothing)
    }

    pub fn cmd_mdw(&mut self, addr: u32) -> Result<simOk, simErr> {
        if self.cli_parser.dut.is_none() {
            self.msgr.error("No differtest");
            return Err(simErr::InvalidCommand);
        }

        let _ = self.sim.mmu.match_memory(simulator::mmu::MatchMsg::ADDR { addr: addr })?;

        self.differtest_watchpoints.push(addr);

        Ok(simOk::Nothing)
    }

    pub fn cmd_t(&mut self) -> Result<simOk, simErr> {
        self.sim.times();
        Ok(simOk::Nothing)
    }
}
