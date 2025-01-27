use simulator::mmu::Mask;
use simulator::Simulator;
use simulator::SimulatorError as simErr;
use simulator::SimulatorOk as simOk;
use std::os::raw::c_void;
use std::result::Result;
use std::sync::atomic::Ordering;

use owo_colors::OwoColorize;

use crate::differtest::DiffertestDirection;
use crate::{
    Monitor, MonitorState,
};

const RV32GPR_NAME: [&'static str; 32] = [
    "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2", "s0", "s1", "a0", "a1", "a2", "a3", "a4",
    "a5", "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11", "t3", "t4",
    "t5", "t6",
];

impl Monitor {
    pub fn cmd_q(&mut self) -> Result<simOk, simErr> {
        self.state = if self.state == MonitorState::TRAP {
            MonitorState::ABORT
        } else {
            MonitorState::QUIT
        };
        Ok(simOk::Nothing)
    }

    pub fn cmd_info(&mut self, target: String, specify: Option<String>) -> Result<simOk, simErr> {
        match target.as_str() {
            "gpr" => {
                if specify.is_none() {
                    for (i, r) in self.sim.cpu_state.gpr.iter().enumerate() {
                        self.msgr.trace(
                            format!("{}: \t0x{:08x}", 
                            RV32GPR_NAME[i].purple(), 
                            r.red()).as_str(),
                        );
                    }
                    return Ok(simOk::Nothing);
                }

                let specify = specify.unwrap();
                
                let index = specify.parse::<u32>();
                if index.is_ok() && (0..32).contains(&index.clone().unwrap()) {
                    let index = index.unwrap();
                    self.msgr.trace(
                        format!(
                            "{}: \t0x{:08x}",
                            "have to impl".purple(),
                            self.sim.cpu_state.gpr[index as usize].red()
                        )
                        .as_str(),
                    );
                    return Ok(simOk::Nothing);
                }
                
                for name in RV32GPR_NAME.iter() {
                    if name == &specify {
                        self.msgr.trace(
                            format!(
                                "{}: \t0x{:08x}",
                                specify.purple(),
                                self.sim.cpu_state.gpr[RV32GPR_NAME.iter().position(|&r| r == specify).unwrap()].red()
                            )
                            .as_str(),
                        );
                        return Ok(simOk::Nothing);
                    }
                    
                }

                self.msgr.error("Invalid index");
                return Err(simErr::InvalidCommand);
            }

            "pc" => {
                self.msgr.trace(
                    format!(
                        "{}: \t0x{:08x}",
                        "pc".purple(),
                        self.sim.cpu_state.pc.red()
                    )
                    .as_str(),
                );
                return Ok(simOk::Nothing);
            }

            "csr" => {
                if specify.is_none() {
                    self.msgr.error("You Have to specify csr index");
                    return Err(simErr::InvalidCommand);
                }

                let specify = specify.unwrap();

                let index = specify.parse::<u32>();

                if index.is_err() {
                    self.msgr.error("index parse error(to u32)");
                    return Err(simErr::InvalidCommand);
                }

                let index = index.unwrap();
                if !(0..4096).contains(&index) {
                    self.msgr.error("Invalid index, should be in 0..4096");
                    return Err(simErr::InvalidCommand);
                }

                self.msgr.trace(
                    format!(
                        "{}{}: \t0x{:08x}",
                        "csr".purple(),
                        index.red(),
                        self.sim.cpu_state.csr[index as usize].red()
                    )
                    .as_str(),
                );

                return Ok(simOk::Nothing);
            }

            _ => {
                self.msgr.error("Invalid target");
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
        } else if self.state == MonitorState::DONE {
            self.msgr.error("Monitor is in done state");
            return Err(simErr::InvalidCommand);
        }

        self.state = MonitorState::RUNNING;

        let count = if count.is_none() { 1 } else { count.unwrap() };

        let mut orig = |trace: bool| -> Result<(), simErr> {
            if self.signal.load(Ordering::SeqCst) {
                return Err(simErr::Signal);
            }

            self.sim.single_instruction(trace)?;

            if self.cli_parser.dut.is_none() {
                return Ok(());
            }

            if self.sim.mmu.is_attch_device == false {
                self.difftest_step()?;
            } else {
                self.differtest.ref_difftest_regcpy(self.sim.cpu_state.gpr.as_ptr() as *mut c_void, DiffertestDirection::ToRef);
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
            let data = self.sim.mmu.read(addr, Mask::None)?;
            self.msgr.trace(format!(" 0x{:08x}: \t0x{:08x}", addr.green(), data.red()).as_str());
            addr = addr + 4;
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
