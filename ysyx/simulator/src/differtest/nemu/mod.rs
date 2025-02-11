use std::sync::Arc;
use parking_lot::{RwLock, Mutex};

use state::{mmu::MMU, reg::{RegIdentifier, RegisterBank, RegisterOps}, ProcessState};

use crate::nemu::Simulator;

use msg_resp::{self as msgr, SimErr};

pub struct DiffertestNemu {
    nemu: Simulator,
    pub mem: Arc<RwLock<MMU>>,
    pub reg: Arc<RwLock<RegisterBank>>,
    pub state: Arc<RwLock<ProcessState>>,
}

impl DiffertestNemu {
    #[cfg(all(feature = "npc", feature = "differtest"))]
    pub fn new() -> Self {
        use ysyx_macro::with_rwlock_write;

        let nemu_resp = Arc::new(Mutex::new(msgr::Resper::new()));
        let mem = Arc::new(RwLock::new(MMU::new(nemu_resp.clone())));
        let reg = Arc::new(RwLock::new(RegisterBank::new()));
        let state = Arc::new(RwLock::new(ProcessState::STOP));

        #[cfg(feature = "npc")]
        with_rwlock_write!(mem, mem_l, {
            mem_l.add_memory("sram", 0x8000_0000, 0x0800_0000);
        });
        #[cfg(feature = "ysyxsoc")]
        with_rwlock_write!(self.mem, mem, {
            mem.add_memory("sram", 0x0f00_0000, 0x0000_2000);
            mem.add_memory("mrom", 0x2000_0000, 0x0000_1000);
            mem.add_memory("flash", 0x3000_0000, 0x1000_0000);
            mem.add_memory("psram", 0x8000_0000, 0x0800_0000);
            mem.add_memory("sdram", 0xa000_0000, 0x0200_0000);
        });

        Self {
            nemu: Simulator::new(mem.clone(), reg.clone(), state.clone()),
            mem: mem.clone(),
            reg: reg.clone(),
            state: state.clone(),
        }
    }

    pub fn ref_difftest_exec(&mut self, n: u32) -> Result<(), SimErr> {
        self.nemu.single_instruction(Some(n))?;
        Ok(())
    }

    pub fn set_ref_reg(&self, dut: RegisterBank) {
        let mut reg = self.reg.write();
        for i in 0..32 {
            let _ = reg.write_gpr(RegIdentifier::Index(i), dut.read_gpr(RegIdentifier::Index(i)).unwrap());

            reg.write_pc(dut.read_pc());
        }
    }
}
