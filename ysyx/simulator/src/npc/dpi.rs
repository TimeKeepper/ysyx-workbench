use dlopen2::wrapper::{Container, WrapperApi};
use ysyx_macro::with_rwlock_write;

use std::sync::{RwLock, OnceLock, Arc};

use state::mmu::{MMU, Mask};

static MEM: OnceLock<Arc<RwLock<MMU>>> = OnceLock::new();

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
pub extern "C" fn rust_sram_write(addr: u32, data: u32) {
    let mem = MEM.get().unwrap();
    
    with_rwlock_write!(mem, mem, {
        mem.write(addr, data, Mask::Word).unwrap();
    });
}
