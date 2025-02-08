use owo_colors::OwoColorize;
use ysyx_macro::{with_rwlock_read, with_rwlock_write};

use std::sync::{atomic::{AtomicU64, Ordering::Relaxed}, Arc, OnceLock, RwLock};

use state::{mmu::{Mask, MMU}, reg::{RegisterBank, RegisterOps}};

use crate::disassembler;

pub static MEM: OnceLock<Arc<RwLock<MMU>>> = OnceLock::new();
pub static REG: OnceLock<Arc<RwLock<RegisterBank>>> = OnceLock::new();

thread_local! {
    pub static DISASM: OnceLock<disassembler::Disassembler> = OnceLock::new();
}

pub static MAP_HIT : AtomicU64 = AtomicU64::new(0);
pub static CACHE_HIT : AtomicU64 = AtomicU64::new(0);
pub static RUN_INST_NUM : AtomicU64 = AtomicU64::new(0);

pub struct InstNum {
    pub calculate: u32,
    pub loadstore: u32,
    pub zicsr: u32,
    pub generalpurpose: u32,
}
pub static mut INST_NUM : InstNum = InstNum {
    calculate: 0,
    loadstore: 0,
    zicsr: 0,
    generalpurpose: 0,
};

pub static ALU_CATCH : AtomicU64 = AtomicU64::new(0);
pub static LSU_CATCH : AtomicU64 = AtomicU64::new(0);

#[no_mangle]
pub extern "C" fn rust_sram_read(addr: u32, data: *mut u32) {
    let mem = MEM.get().unwrap();
    
    with_rwlock_write!(mem, mem, {
        unsafe {
            *data = mem.read(addr, Mask::Word).unwrap();
        }
    });
}

#[no_mangle]
pub extern "C" fn rust_sram_write(addr: u32, data: u32, strb: u32) {
    let mem = MEM.get().unwrap();
    
    with_rwlock_write!(mem, mem, {
        let mask = match strb {
            0b0001 | 0b0010 | 0b0100 | 0b1000 => Mask::Byte,
            0b0011 | 0b0110 | 0b1100 => Mask::Half,
            0b1111 => Mask::Word,
            _ => panic!("Invalid strb value: {}", strb),
        };
        let data = match strb {
            0b0010 => data.rotate_right(8),
            0b0100 | 0b1100 => data.rotate_right(16),
            0b1000 => data.rotate_right(24),
            _ => data,
        };
        mem.write(addr, data, mask).unwrap();
    });
}

#[no_mangle]
pub extern "C" fn rust_IFU_catch(inst: u32) {
    DISASM.with(|cell| {
        let disasm = cell.get().unwrap();

        let pc = with_rwlock_read!(REG.get().unwrap(), reg, {
            reg.read_pc()
        });

        let result = disasm
            .disasm(&inst.to_le_bytes(), pc as u64)
            .replace("\0", "")
            .trim()
            .split_ascii_whitespace()
            .map(|x| format!("{} ", x))
            .collect::<String>();

        println!("{:08x}: {:08x} {}", pc.purple(), inst.red(), result.green());
    });
}

#[no_mangle]
pub extern "C" fn rust_Icache_catch(map_hit: u32, cache_hit: u32) {
    MAP_HIT.fetch_add(if map_hit != 0 {1} else {0}, Relaxed);
    CACHE_HIT.fetch_add(if cache_hit != 0 {1} else {0}, Relaxed);
}

#[no_mangle]
pub extern "C" fn rust_IDU_catch(r#type: u32) {
    match r#type {
        0 => unsafe {
            INST_NUM.calculate += 1;
        },
        1 => unsafe {
            INST_NUM.loadstore += 1;
        },
        2 => unsafe {
            INST_NUM.zicsr += 1;
        },
        3 => unsafe {
            INST_NUM.generalpurpose += 1;
        },
        _ => panic!("Invalid IDU type: {}", r#type),
    }
}

#[no_mangle]
pub extern "C" fn rust_ALU_catch() {
    ALU_CATCH.fetch_add(1, Relaxed);
}

#[no_mangle]
pub extern "C" fn rust_LSU_catch() {
    LSU_CATCH.fetch_add(1, Relaxed);
}

#[no_mangle]
pub extern "C" fn rust_WBU_catch(next_pc: u32, 
    gpr_waddr: u32, gpr_wdata: u32, 
    csr_wena: u32, csr_waddra: u32, csr_wdataa: u32,
    csr_wenb: u32, csr_waddrb: u32, csr_wdatab: u32) {
    let reg = REG.get().unwrap();
    with_rwlock_write!(reg, reg, {
        reg.write_pc(next_pc);
        reg.write_gpr(state::reg::RegIdentifier::Index(gpr_waddr as usize), gpr_wdata).unwrap();
        if csr_wena != 0 {
            reg.write_csr(state::reg::RegIdentifier::Index(csr_waddra as usize), csr_wdataa).unwrap();
        }
        if csr_wenb != 0 {
            reg.write_csr(state::reg::RegIdentifier::Index(csr_waddrb as usize), csr_wdatab).unwrap();
        }
    });

    RUN_INST_NUM.store(RUN_INST_NUM.load(Relaxed).saturating_sub(1), Relaxed);
}
