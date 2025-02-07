// use msg_resp::{CtrlCommand, ResultMessage, SimErr, SimOk};
use super::msg_dependencies::*;
use super::state_dependencies::*;
use ysyx_macro::{with_rwlock_read, with_rwlock_write};

use owo_colors::OwoColorize;
use super::monitor_parser::Commands as Cmd;

use crate::Monitor;

impl Monitor {
    pub fn execute(&mut self, cmd: Cmd) {
        match cmd {
            Cmd::Quit {  } => {
                self.cmd_q();
            }

            Cmd::State {  } => {
                self.cmd_s();
            }

            Cmd::SingleInstrcution { count } => {
                self.cmd_si(count);
            }

            Cmd::Continue {  } => {
                self.cmd_c();
            }

            Cmd::Info { target, index } => {
                let specify = if index.is_none() { None } else { Some(index.unwrap().to_string()) };
                self.cmd_info(target, specify);
            }

            Cmd::Function { on_or_off, target } => {
                self.cmd_func(on_or_off, target);
            }

            Cmd::InstructionRingBuffer {  } => {
                self.cmd_ir();
            }

            Cmd::Times {  } => {
                self.cmd_t();
            }

            Cmd::Examine { addr, length } => {
                let result = self.cmd_x(addr, length);
                self.deal_result(result);
            }

            Cmd::MemoryMap {  } => {
                self.cmd_mm();
            }

            Cmd::MemoryDiffertestWatchpoint { addr } => {
                let result = self.cmd_mdw(addr);
                self.deal_result(result);
            }

            Cmd::Receive {  } => {
                self.cmd_r();
            }

            // _ => {
            //     self.resper.lock().unwrap().trace(format!("{:?}", cmd).as_str());
            // }
        }
    }

    #[cfg(feature = "nemu")]
    pub fn nemu_start_decode(&mut self) {
        assert!(matches!(self.cmd_send(CtrlCommand::FUNC { on_or_off: Some(true), target: Some("decode".to_string()) }), Ok(SimOk::FunctionCtrl)));
    }

    pub fn batch(&mut self) {
        self.resper.lock().unwrap().info("Execute in Batch mode");
        let _ = self.cmd_c();
        self.cmd_q();
    }

    pub fn cmd_send(&mut self, cmd: CtrlCommand) -> ResultMessage {
        self.cmd_sender.send(cmd).unwrap();
        self.result_receiver.recv().unwrap()
    }

    fn deal_result(&mut self, result: ResultMessage) {
        match result {
            Ok(_) => {with_rwlock_write!(self.state, state, { state.set(ProcessState::STOP); });},
            Err(err) => match err {
                SimErr::Ebreak { is_good } => {
                    with_rwlock_write!(self.state, state, {
                        *state = if is_good {
                            self.resper.lock().unwrap().success("Hit Good TRAP");
                            ProcessState::DONE
                        } else {
                            self.resper.lock().unwrap().error("Hit Bad TRAP");
                            ProcessState::TRAP
                        }
                    });
                }

                SimErr::NotImplemented => self.resper.lock().unwrap().error("Not implemented yet"),
                SimErr::InvalidCommand => self.resper.lock().unwrap().error("Invalid command"),
                SimErr::InvalidRegIndentifier => self
                    .resper
                    .lock()
                    .unwrap()
                    .error("Invalid register identifier"),

                SimErr::DiffertestFailed => {
                    self.resper.lock().unwrap().error("Differtest failed");
                    with_rwlock_write!(self.state, state, { *state = ProcessState::TRAP });
                }
                SimErr::NoMatchingMemory {} => {
                    with_rwlock_write!(self.state, state, { *state = ProcessState::TRAP });
                }
                SimErr::NoMatchingDevice {} => {
                    with_rwlock_write!(self.state, state, { *state = ProcessState::TRAP });
                }
                SimErr::InstrctionDecodeFailed {} => {
                    with_rwlock_write!(self.state, state, { *state = ProcessState::TRAP });
                }
                SimErr::InstrctionExecuteFailed {} => {
                    self.resper
                        .lock()
                        .unwrap()
                        .error(format!("Failed to execute instrcution").as_str());
                    with_rwlock_write!(self.state, state, { *state = ProcessState::TRAP });
                }

                _ => {
                    self.resper.lock().unwrap().error(format!("{:?}", err).as_str());
                    with_rwlock_write!(self.state, state, { *state = ProcessState::TRAP });
                }
            },
        }
    }
    
    fn cmd_q(&mut self) {
        with_rwlock_write!(self.state, state, {
            if *state == ProcessState::TRAP {
                *state = ProcessState::ABORT;
            } else {
                *state = ProcessState::QUIT;
            }
        });

        assert!(matches!(self.cmd_send(CtrlCommand::QUIT), Ok(SimOk::Quit)));
    }

    fn cmd_s(&mut self) {
        self.resper.lock().unwrap().trace(format!("{:?}", self.state.read().unwrap()).as_str());
    }

    fn cmd_r(&mut self) {
        self.resper.lock().unwrap().trace(format!("{:?}", self.result_receiver.try_recv()).as_str());
    }

    fn cmd_info(&mut self, target: String, specify: Option<String>){
        let result: ResultMessage;
        match target.as_str() {
            "gp" => {
                result = self.reg.read().unwrap().print_reg(specify, RegType::GPR);
            }

            "pc" => {
                println!("{}: \t0x{:08x}", "pc".purple(), self.reg.read().unwrap().read_pc().red());
                return;
            }

            "cs" => {
                result = self.reg.read().unwrap().print_reg(specify, RegType::CSR);
            }

            _ => {
                self.resper.lock().unwrap().error("Invalid register identifier");
                self.resper.lock().unwrap().important("Valid identifiers: gp, pc, cs");
                return;
            }
        }
        self.deal_result(result);
    }

    fn cmd_func(
        &mut self,
        on_or_off: Option<bool>,
        target: Option<String>,
    ) {
        assert!(matches!(self.cmd_send(CtrlCommand::FUNC { on_or_off, target }), Ok(SimOk::FunctionCtrl) | Ok(SimOk::FunctionShow)));
    }

    fn cmd_si(&mut self, count: Option<u32>) {
        self.state.write().unwrap().set(ProcessState::RUNNING);
        let result = self.cmd_send(CtrlCommand::SI { count });
        self.deal_result(result);
    }

    fn cmd_c(&mut self) {
        self.cmd_si(Some(0))
    }

    fn cmd_ir(&mut self) {
        assert!(matches!(self.cmd_send(CtrlCommand::DEBUG { target: "ir".to_string() }), Ok(SimOk::DebugTrace)));
    }

    fn cmd_x(&mut self, addr: u32, length: Option<u32>) -> ResultMessage {
        let mut addr = addr;
        let length = if length.is_none() { 1 } else { length.unwrap() };

        for _ in 0..length {
            let data = self.mem.read().unwrap().read(addr, state::mmu::Mask::None)?;
            self.resper.lock().unwrap().trace(format!(" 0x{:08x}: \t0x{:08x}", addr.green(), data.red()).as_str());
            addr = addr + 4;
        }
        
        Ok(SimOk::Nothing)
    }

    fn cmd_mm(&mut self){
        with_rwlock_read!(self.mem, mem, {
            mem.memory_map();
        });
    }

    fn cmd_mdw(&mut self, _addr: u32) -> ResultMessage {
        // if self.cli_parser.dut.is_none() {
        //     self.msgr.error("No differtest");
        //     return Err(SimErr::InvalidCommand);
        // }

        // let _ = self.sim.get_mem_state().match_memory(MatchMsg::ADDR { addr })?;

        // self.differtest_watchpoints.push(addr);

        Ok(SimOk::Nothing)
    }

    fn cmd_t(&mut self) {
        assert!(matches!(self.cmd_send(CtrlCommand::DEBUG { target: "t".to_string() }), Ok(SimOk::DebugTrace)));
    }
}
