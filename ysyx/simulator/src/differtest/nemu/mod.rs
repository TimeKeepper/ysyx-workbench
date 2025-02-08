use std::sync::{Arc, RwLock};

use state::{mmu::MMU, reg::RegisterBank, ProcessState};

use crate::nemu::Simulator;

pub struct DiffertestNemu {
    nemu: Simulator,
    pub mem: Arc<RwLock<MMU>>,
    pub reg: Arc<RwLock<RegisterBank>>,
    pub state: Arc<RwLock<ProcessState>>,
}

impl DiffertestNemu {
    // pub fn new() -> Self {
    //     let mem = Arc::new(RwLock::new(MMU::new()));

    //     Self {
    //         Simulator::new(
    //             cmd_receiver,
    //             result_sender,
    //             mem.clone(),
    //             reg.clone(),
    //             state.clone(),
    //             resper.clone(),
    //         )
            
    //     }
    // }

    // pub fn differtest_step(&mut self) {
    //     self.nemu.single_instruction(Some(1)).unwrap();
    // }
}