use std::os::raw::c_void;

use msg_resp::SimErr;
use owo_colors::OwoColorize;
use state::reg::{RegIdentifier, RegisterOps};
use ysyx_macro::with_rwlock_read;

use crate::Simulator;

#[cfg(feature = "differtest")]
impl Simulator {
    pub fn difftest_step(&mut self) -> Result<(), SimErr> {
        self.differtest.ref_difftest_exec(1);
        
        let ref_r = self.differtest.get_ref_reg();

        with_rwlock_read!(self.reg, reg, {
            for gpr in 0..32 {
                let dut_data = reg.read_gpr(RegIdentifier::Index(gpr))?;
                if dut_data != ref_r.gpr[gpr] {
                    println!("Differtest failed");
                    println!("DUT x{}: {:08x}", gpr.red(), dut_data.purple());
                    println!("REF x{}: {:08x}", gpr.red(), ref_r.gpr[gpr].purple());
                    return Err(SimErr::DiffertestFailed);
                }
            }
            let dut_pc = reg.read_pc();
            if dut_pc != ref_r.pc {
                println!("Differtest failed");
                println!("DUT pc: {:08x}", dut_pc.purple());
                println!("REF pc: {:08x}", ref_r.pc.purple());
                return Err(SimErr::DiffertestFailed);
            }
            for csr in 0..4096 {
                let dut_data = reg.read_csr(RegIdentifier::Index(csr))?;
                if dut_data != ref_r.csr[csr] {
                    println!("Differtest failed");
                    println!("DUT csr[{}]: {:08x}", csr.red(), dut_data.purple());
                    println!("REF csr[{}]: {:08x}", csr.red(), ref_r.csr[csr].purple());
                    return Err(SimErr::DiffertestFailed);
                }
            }
        });

        // for w in self.differtest_watchpoints.iter() {
        //     let dut_data = self.sim.get_mem_state().read(*w, simulator::mmu::Mask::None)?;
        //     let ref_data = {
        //         let mut data = 0u32;
        //         self.differtest.ref_difftest_memcpy(*w as u64, &mut data as *mut u32 as *mut c_void, 4, super::DiffertestDirection::ToDut);
        //         data
        //     };

        //     if dut_data != ref_data {
        //         println!("Differtest failed");
        //         println!("DUT 0x{:08x}: {:08x}", w.red(), dut_data.purple());
        //         println!("REF 0x{:08x}: {:08x}", w.red(), ref_data.purple());
        //         return Err(SimErr::DiffertestFailed);
        //     }
        // }

        Ok(())
    }
}
