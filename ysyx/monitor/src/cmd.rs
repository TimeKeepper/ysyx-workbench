use msg_resp::{MatchMsg, CtrlCommand, ResultMessage, SimErr, SimOk};
use state::reg::{RegType, RegisterOps};
use state::ProcessState;
use ysyx_macro::{with_rwlock_read, with_rwlock_write};
use std::os::raw::c_void;
use std::result::Result;
use std::sync::atomic::Ordering;

use owo_colors::OwoColorize;

use crate::Monitor;

impl Monitor {
    pub fn cmd_q(&mut self) -> ResultMessage {
        with_rwlock_write!(self.state, state, {
            if *state == ProcessState::TRAP {
                *state = ProcessState::ABORT;
            } else {
                *state = ProcessState::QUIT;
            }
        });

        self.cmd_sender.send(CtrlCommand::QUIT).unwrap();

        self.result_receiver.recv().unwrap()
    }

    pub fn cmd_info(&mut self, target: String, specify: Option<String>) -> ResultMessage {
        match target.as_str() {
            "gp" => {
                self.reg.read().unwrap().print_reg(specify, RegType::GPR)
            }

            "pc" => {
                println!("{}: \t0x{:08x}", "pc".purple(), self.reg.read().unwrap().read_pc().red());
                return Ok(SimOk::Nothing);
            }

            "cs" => {
                self.reg.read().unwrap().print_reg(specify, RegType::CSR)
            }

            _ => {
                self.resper.lock().unwrap().error("Invalid register identifier");
                self.resper.lock().unwrap().important("Valid identifiers: gp, pc, cs");
                return Err(SimErr::InvalidCommand);
            }
        }
    }

    pub fn cmd_func(
        &mut self,
        on_or_off: bool,
        target: Option<String>,
    ) -> ResultMessage {
        self.cmd_sender.send(CtrlCommand::FUNC { on_or_off, target }).unwrap();
        Ok(SimOk::Nothing)
    }

    pub fn cmd_si(&mut self, count: Option<u32>) -> ResultMessage {
        // if self.state == MonitorState::TRAP {
        //     self.msgr.error("Monitor is in trap state");
        //     return Err(SimErr::InvalidCommand);
        // } else if self.state == MonitorState::DONE {
        //     self.msgr.error("Monitor is in done state");
        //     return Err(SimErr::InvalidCommand);
        // }

        // self.state = MonitorState::RUNNING;

        // let count = if count.is_none() { 1 } else { count.unwrap() };

        // let mut orig = |trace: bool| -> Result<(), SimErr> {
        //     if self.signal.load(Ordering::SeqCst) {
        //         return Err(SimErr::Signal);
        //     }

        //     self.sim.single_instruction(trace)?;

        //     if self.cli_parser.dut.is_none() {
        //         return Ok(());
        //     }

        //     if self.sim.external_state_change() {
        //         self.difftest_step()?;
        //     } else {
        //         self.differtest.ref_difftest_regcpy(self.sim.get_reg_state().gpr.as_ptr() as *mut c_void, DiffertestDirection::ToRef);
        //     }

        //     Ok(())
        // };

        // if count == 0 {
        //     loop {
        //         orig(false)?;
        //     }
        // }

        // for _ in 0..count {
        //     orig(count < 10)?;
        // }

        // Ok(SimOk::InstructionExecuted)
        
        self.cmd_sender.send(CtrlCommand::SI { count }).unwrap();
        self.result_receiver.recv().unwrap()
    }

    pub fn cmd_c(&mut self) -> ResultMessage {
        self.cmd_si(Some(0))
    }

    pub fn cmd_ir(&mut self) -> ResultMessage {
        // self.sim.instruction_ring_buffer();
        Ok(SimOk::Nothing)
    }

    pub fn cmd_x(&mut self, addr: u32, length: Option<u32>) -> ResultMessage {
        let mut addr = addr;
        let length = if length.is_none() { 1 } else { length.unwrap() };

        for _ in 0..length {
            let data = self.mem.read().unwrap().read(addr, state::mmu::Mask::None)?;
            self.resper.lock().unwrap().trace(format!(" 0x{:08x}: \t0x{:08x}", addr.green(), data.red()).as_str());
            addr = addr + 4;
        }
        
        Ok(SimOk::Nothing)
    }

    pub fn cmd_mm(&mut self) -> ResultMessage {
        with_rwlock_read!(self.mem, mem, {
            mem.memory_map();
        });
        Ok(SimOk::Nothing)
    }

    pub fn cmd_mdw(&mut self, addr: u32) -> ResultMessage {
        // if self.cli_parser.dut.is_none() {
        //     self.msgr.error("No differtest");
        //     return Err(SimErr::InvalidCommand);
        // }

        // let _ = self.sim.get_mem_state().match_memory(MatchMsg::ADDR { addr })?;

        // self.differtest_watchpoints.push(addr);

        Ok(SimOk::Nothing)
    }

    pub fn cmd_t(&mut self) -> ResultMessage {
        // self.sim.times();
        Ok(SimOk::Nothing)
    }
}
