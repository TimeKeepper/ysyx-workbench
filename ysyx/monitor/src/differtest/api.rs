use std::os::raw::c_void;

use super::Riscv32CpuState;
use simulator::SimulatorError as simErr;
use owo_colors::OwoColorize;

use crate::Monitor;

impl Monitor {
    pub fn difftest_step(&mut self) -> Result<(), simErr> {
        self.differtest.ref_difftest_exec(1);
        
        let dut_r = Riscv32CpuState {
            gpr: {
                let mut gpr_array = [0u32; 32];
                let gpr_vec = self.sim.cpu_state.gpr.iter().map(|r| r.value).collect::<Vec<u32>>();
                gpr_array.copy_from_slice(&gpr_vec[..32]);
                gpr_array
            },
            pc: self.sim.cpu_state.pc.value,
            csr: [0; 4096],
        };

        let ref_r = self.differtest.get_ref_reg();

        for gpr in 0..32 {
            if dut_r.gpr[gpr] != ref_r.gpr[gpr] {
                println!("Differtest failed");
                println!("DUT x{}: {:08x}", gpr.red(), dut_r.gpr[gpr].purple());
                println!("REF x{}: {:08x}", gpr.red(), ref_r.gpr[gpr].purple());
                return Err(simErr::DiffertestFailed);
            }
        }
        if dut_r.pc != ref_r.pc {
            println!("Differtest failed");
            println!("DUT pc: {:08x}", dut_r.pc.purple());
            println!("REF pc: {:08x}", ref_r.pc.purple());
            return Err(simErr::DiffertestFailed);
        }

        for w in self.differtest_watchpoints.iter() {
            let dut_data = self.sim.mmu.read(*w, simulator::mmu::Mask::None)?;
            let ref_data = {
                let mut data = 0u32;
                self.differtest.ref_difftest_memcpy(*w as u64, &mut data as *mut u32 as *mut c_void, 4, super::DiffertestDirection::ToDut);
                data
            };

            if dut_data != ref_data {
                println!("Differtest failed");
                println!("DUT 0x{:08x}: {:08x}", w.red(), dut_data.purple());
                println!("REF 0x{:08x}: {:08x}", w.red(), ref_data.purple());
                return Err(simErr::DiffertestFailed);
            }
        }

        Ok(())
    }
}
