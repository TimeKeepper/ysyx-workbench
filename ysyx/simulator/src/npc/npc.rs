use msg_resp::{SimOk, SimErr};
use msg_resp::{CtrlCommand, ResultMessage};
use msg_resp as msgr;
use std::sync::atomic::Ordering;
use std::sync::mpsc::Receiver;
use std::sync::mpsc::Sender;
use std::sync::{Arc, Mutex, RwLock};

use state::mmu::MMU;
use state::reg::RegisterBank;
use state::ProcessState;
    
use super::dl::NpcWrapper;
use super::dpi;

pub struct Simulator {
    pub resper: Arc<Mutex<msgr::Resper>>,

    pub cmd_receiver: Receiver<CtrlCommand>,
    pub result_sender: Sender<ResultMessage>,

    pub mem: Arc<RwLock<MMU>>,
    pub reg: Arc<RwLock<RegisterBank>>,
    pub state: Arc<RwLock<ProcessState>>,

    pub wrapper: NpcWrapper
    // pub cont: *const Container<Api>,
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

        Self {
            resper,

            cmd_receiver,
            result_sender,

            mem,
            reg,
            state,

            wrapper,
        }
    }

    pub fn run(self) {
        loop {
            let result = self.cmd_receiver.recv();

            match result {
                Ok(CtrlCommand::QUIT) => {
                    self.result_sender.send(Ok(SimOk::Quit)).unwrap();
                    break;
                }

                Ok(CtrlCommand::SI { count }) => {
                    let count = count.unwrap_or(1);

                    dpi::RUN_INST_NUM.fetch_add(count as u64, Ordering::SeqCst);

                    loop {
                        if dpi::RUN_INST_NUM.load(Ordering::SeqCst) == 0 {
                            break;
                        }
                        self.wrapper.single_cycle();
                    }

                    self.result_sender.send(Ok(SimOk::Nothing)).unwrap();
                }

                Ok(CtrlCommand::DIFFERTEST { path, length }) => {
                    self.result_sender.send(Err(SimErr::DiffertestFailedToInit)).unwrap();
                }

                _ => {
                    self.resper.lock().unwrap().error("Invalid command");
                    self.result_sender.send(Err(SimErr::InvalidCommand)).unwrap();
                }
            }
        }
    }
}
