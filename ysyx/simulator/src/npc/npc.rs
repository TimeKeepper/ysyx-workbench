use crate::npc::RUN_INST_NUM;
use crate::{differtest, disassembler};
use msg_resp::{MatchMsg, SimErr, SimOk};
use msg_resp::{CtrlCommand, ResultMessage};
use msg_resp as msgr;
use std::sync::atomic::Ordering;
use std::sync::mpsc::Receiver;
use std::sync::mpsc::Sender;
use std::sync::Arc;

use parking_lot::{Mutex, RwLock};

use state::mmu::MMU;
use state::reg::RegisterBank;
use state::ProcessState;

use super::dl::NpcWrapper;
use super::dpi;

pub struct Simulator {
    pub resper: Arc<Mutex<msgr::Resper>>,

    pub disasm: disassembler::Disassembler,

    pub cmd_receiver: Receiver<CtrlCommand>,
    pub result_sender: Sender<ResultMessage>,

    pub mem: Arc<RwLock<MMU>>,
    pub reg: Arc<RwLock<RegisterBank>>,
    pub state: Arc<RwLock<ProcessState>>,

    pub wrapper: NpcWrapper,

    #[cfg(all(feature = "npc", feature = "differtest"))]
    pub differtest: differtest::Differtest,
}

impl Simulator {
    pub fn new(cmd_receiver: Receiver<CtrlCommand>, result_sender: Sender<ResultMessage>, 
        mem: Arc<RwLock<state::mmu::MMU>>, 
        reg: Arc<RwLock<state::reg::RegisterBank>>,
        state: Arc<RwLock<ProcessState>>,
        resper: Arc<Mutex<msgr::Resper>>) -> Self {

        let wrapper = NpcWrapper::new("/home/wenjiu/ysyx-workbench/npc/platform/npc/build/so_obj_dir/libtop.so");

        wrapper.init_sram_read(dpi::rust_sram_read);
        wrapper.init_sram_write(dpi::rust_sram_write);
        wrapper.init_ifu_catch(dpi::rust_IFU_catch);
        wrapper.init_icache_catch(dpi::rust_Icache_catch);
        wrapper.init_idu_catch(dpi::rust_IDU_catch);
        wrapper.init_alu_catch(dpi::rust_ALU_catch);
        wrapper.init_lsu_catch(dpi::rust_LSU_catch);
        wrapper.init_wbu_catch(dpi::rust_WBU_catch);
        wrapper.init(0, std::ptr::null());

        wrapper.reset(50);

        dpi::MEM.get_or_init(|| mem.clone());
        dpi::REG.get_or_init(|| reg.clone());
        
        let disasm = disassembler::Disassembler::new("riscv32");

        dpi::DISASM.with(|cell| {
            cell.get_or_init(|| disasm.clone());
        });

        let differtest = differtest::Differtest::new();

        Self {
            resper,

            disasm,

            cmd_receiver,
            result_sender,

            mem,
            reg,
            state,

            wrapper,

            differtest,
        }
    }

    pub fn run(mut self) {
        'main_loop: loop {
            let result = self.cmd_receiver.recv();

            match result {
                Ok(CtrlCommand::QUIT) => {
                    self.result_sender.send(Ok(SimOk::Quit)).unwrap();
                    break;
                }

                Ok(CtrlCommand::SI { count }) => {
                    let count = count.unwrap_or(1);

                    dpi::RUN_INST_NUM.fetch_add(count as u64, Ordering::SeqCst);

                    let mut count = dpi::RUN_INST_NUM.load(Ordering::SeqCst);

                    if count == 0 {
                        '_si: loop {
                            let result = self.difftest_step()
                                .map_err(|e| {
                                    match e {
                                        SimErr::Ebreak { is_good } => {
                                            self.resper.lock().error(format!("Differtest TRAP").as_str());
                                            self.state.write().set(ProcessState::DONE);
                                        }
                                        _ => {
                                            self.resper.lock().error(format!("{:?}", e).as_str());
                                            self.state.write().set(ProcessState::TRAP);
                                        }
                                    }

                                    e
                                }).is_err();
                            if result {
                                self.result_sender.send(Ok(SimOk::Nothing)).unwrap();
                                continue 'main_loop;
                            }
                        }
                    }

                    '_si: loop {
                        let cur = dpi::RUN_INST_NUM.load(Ordering::SeqCst);
                        if cur == (count - 1) {
                            count -= 1;
                            let result = self.difftest_step()
                                .map_err(|e| self.resper.lock().error(format!("{:?}", e).as_str()));

                            if result.is_err() {
                                break '_si;
                            }
                        
                            if cur == 0 {
                                break '_si;
                            }
                        }
                        self.wrapper.single_cycle();
                    }

                    self.result_sender.send(Ok(SimOk::Nothing)).unwrap();
                }

                Ok(CtrlCommand::DIFFERTEST { _path, _length }) => {
                    #[cfg(not(feature = "differtest"))]
                    self.result_sender.send(Err(SimErr::DiffertestFailedToInit)).unwrap();

                    #[cfg(feature = "differtest")]
                    {
                        self.differtest.set_ref_reg(self.reg.read().clone());

                        let result = self.differtest.mem.write().load(
                            "sram", 
                            &self.mem.read()
                                .match_memory(MatchMsg::NAME { name: "sram".to_string() })
                                .ok().unwrap().memory
                                [0.._length as usize]);

                        if result.is_err() {
                            self.result_sender.send(Err(SimErr::DiffertestFailedToInit)).unwrap();
                        } else {
                            self.result_sender.send(Ok(SimOk::DiffertestInit)).unwrap();
                        }
                    }
                }

                Ok(CtrlCommand::FUNC { on_or_off, target }) => {
                    self.result_sender.send(Ok(SimOk::FunctionCtrl)).unwrap();
                }

                _ => {
                    self.resper.lock().error("Invalid command");
                    self.result_sender.send(Err(SimErr::InvalidCommand)).unwrap();
                }
            }
        }
    }
}
