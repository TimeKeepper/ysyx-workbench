use std::os::raw::c_void;

use dlopen2::wrapper::{Container, WrapperApi};
use state::reg::RegisterBank;

use super::DiffertestDirection;

#[derive(WrapperApi)]
struct Api {
    difftest_memcpy: unsafe extern "C" fn(addr: u64, buf: *mut c_void, n: u64, direction: bool),
    difftest_regcpy: unsafe extern "C" fn(buf: *mut c_void, direction: bool),
    difftest_exec: unsafe extern "C" fn(n: u64),
    difftest_raise_intr: unsafe extern "C" fn(n: u64),
    difftest_init: unsafe extern "C" fn(port: u32),
}

pub struct DiffertestDl {
    cont: Option<Container<Api>>,
}

/// siutable for differtest
#[cfg(feature = "riscv32")]
#[repr(C)]
pub struct DiffertestRegBank {
    pub gpr: [u32; 32],
    pub pc: u32,
    pub csr: [u32; 4096],
}

#[cfg(feature = "riscv32")]
impl DiffertestRegBank {
    pub fn new() -> Self {
        Self {
            gpr: [0; 32],
            pc: 0,
            csr: [0; 4096],
        }
    }
}

impl DiffertestDl {
    pub fn new() -> Self {
        // let cont: Container<Api> = unsafe { Container::load(path).expect("Could not open library or load symbols")}
        Self {
            cont: None,
        }
    }

    pub fn init(&mut self, path: &str) {
        self.cont = Some(unsafe { Container::load(path).expect("Could not open library or load symbols") });
    }

    pub fn get_ref_reg(&self) -> DiffertestRegBank {
        let ref_r = DiffertestRegBank::new();

        let ref_r_ptr = &ref_r as *const DiffertestRegBank as *mut c_void;
        unsafe {
            self.cont.as_ref().expect("?").difftest_regcpy(ref_r_ptr, DiffertestDirection::ToDut.into());
        }

        ref_r
    }

    pub fn set_ref_reg(&self, executor: &RegisterBank) {
        unsafe {
            let ref_r = DiffertestRegBank{
                gpr: executor.gp,
                pc: executor.pc,
                csr: executor.cs,
            };
            let ref_r_ptr = &ref_r as *const DiffertestRegBank as *mut c_void;
            // let ref_r_ptr = executor as *const DiffertestRegBank as *mut c_void;
            (self.cont.as_ref().expect("?").difftest_regcpy)(ref_r_ptr, DiffertestDirection::ToRef.into());
        }
    }

    pub fn ref_difftest_memcpy(
        &self,
        addr: u64,
        buf: *mut c_void,
        n: u64,
        direction: DiffertestDirection,
    ) {
        unsafe {
            (self.cont.as_ref().expect("?").difftest_memcpy)(addr, buf, n, direction.into());
        }
    }

    pub fn ref_difftest_regcpy(&self, buf: *mut c_void, direction: DiffertestDirection) {
        unsafe {
            (self.cont.as_ref().expect("?").difftest_regcpy)(buf, direction.into());
        }
    }

    pub fn ref_difftest_exec(&self, n: u64) {
        unsafe {
            (self.cont.as_ref().expect("?").difftest_exec)(n);
        }
    }

    pub fn ref_difftest_raise_intr(&self, n: u64) {
        unsafe {
            (self.cont.as_ref().expect("?").difftest_raise_intr)(n);
        }
    }

    pub fn ref_difftest_init(&self, port: u32) {
        unsafe {
            (self.cont.as_ref().expect("?").difftest_init)(port);
        }
    }
}
