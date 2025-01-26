
use libloading::{Library, Symbol};
use std::{os::raw::c_void, path::Path, sync::OnceLock};

use simulator::nemu::Riscv32CpuState as nemuState;

pub enum DiffertestDirection {
    ToDut = 0,
    ToRef = 1,
}

// case DiffertestDirection to bool
impl From<DiffertestDirection> for bool {
    fn from(direction: DiffertestDirection) -> Self {
        match direction {
            DiffertestDirection::ToDut => false,
            DiffertestDirection::ToRef => true,
        }
    }
}

type RefDifftestMemcpyFunc =
    unsafe extern "C" fn(addr: u64, buf: *mut c_void, n: u64, direction: bool);
type RefDifftestRegcpyFunc = unsafe extern "C" fn(buf: *mut c_void, direction: bool);
type RefDifftestExecFunc = unsafe extern "C" fn(n: u64);
type RefDifftestRaiseIntrFunc = unsafe extern "C" fn(n: u64);
type RefDifftestInit = unsafe extern "C" fn(port: u32);

static LIBRARY: OnceLock<Library> = OnceLock::new();
static REF_DIFTEST_MEMCPY: OnceLock<Symbol<'static, RefDifftestMemcpyFunc>> = OnceLock::new();
static REF_DIFTEST_REGCPY: OnceLock<Symbol<'static, RefDifftestRegcpyFunc>> = OnceLock::new();
static REF_DIFTEST_EXEC: OnceLock<Symbol<'static, RefDifftestExecFunc>> = OnceLock::new();
static REF_DIFTEST_RAISE_INTR: OnceLock<Symbol<'static, RefDifftestRaiseIntrFunc>> =
    OnceLock::new();
static REF_DIFTEST_INIT: OnceLock<Symbol<'static, RefDifftestInit>> = OnceLock::new();

#[derive(Debug, PartialEq)]
#[repr(C)]
pub struct Riscv32CpuState {
    pub gpr: [u32; 32],
    pub pc: u32,
    pub csr: [u32; 4096],
}

pub struct Differtest;

impl Differtest {
    pub fn new() -> Self {
        Self
    }

    pub fn get_ref_reg(&self) -> Riscv32CpuState {
        let ref_r = Riscv32CpuState {
            gpr: [0; 32],
            pc: 0,
            csr: [0; 4096],
        };

        let ref_r_ptr = &ref_r as *const Riscv32CpuState as *mut c_void;
        unsafe {
            REF_DIFTEST_REGCPY.get().expect("wtf")(ref_r_ptr, DiffertestDirection::ToDut.into());
        }

        ref_r
    }

    pub fn set_ref_reg(&self, executor: &nemuState) {
        unsafe {
            let mut ref_r = Riscv32CpuState {
                gpr: [0; 32],
                pc: 0,
                csr: [0; 4096],
            };

            ref_r
                .gpr
                .copy_from_slice(&executor.gpr.iter().map(|r| r.value).collect::<Vec<u32>>()[..]);
            ref_r.pc = executor.pc.value;

            let ref_r_ptr = &ref_r as *const Riscv32CpuState as *mut c_void;
            REF_DIFTEST_REGCPY.get().unwrap()(ref_r_ptr, DiffertestDirection::ToRef.into());
        }
    }

    pub fn init(&self, path: &str) {
        unsafe {
            let path = Path::new(path);
            // let path = Path::new("/home/wenjiu/ysyx-workbench/nemu/tools/spike-diff/build/riscv32-spike-so");
            let lib = Library::new(path).unwrap();
            LIBRARY.set(lib).unwrap();
            let ref_difftest_memcpy: Symbol<RefDifftestMemcpyFunc> =
                LIBRARY.get().unwrap().get(b"difftest_memcpy").unwrap();
            let ref_difftest_regcpy: Symbol<RefDifftestRegcpyFunc> =
                LIBRARY.get().unwrap().get(b"difftest_regcpy").unwrap();
            let ref_difftest_exec: Symbol<RefDifftestExecFunc> =
                LIBRARY.get().unwrap().get(b"difftest_exec").unwrap();
            let ref_difftest_raise_intr: Symbol<RefDifftestRaiseIntrFunc> =
                LIBRARY.get().unwrap().get(b"difftest_raise_intr").unwrap();
            let ref_difftest_init: Symbol<RefDifftestInit> =
                LIBRARY.get().unwrap().get(b"difftest_init").unwrap();

            REF_DIFTEST_MEMCPY.set(ref_difftest_memcpy).unwrap();
            REF_DIFTEST_REGCPY.set(ref_difftest_regcpy).unwrap();
            REF_DIFTEST_EXEC.set(ref_difftest_exec).unwrap();
            REF_DIFTEST_RAISE_INTR.set(ref_difftest_raise_intr).unwrap();
            REF_DIFTEST_INIT.set(ref_difftest_init).unwrap();
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
            REF_DIFTEST_MEMCPY.get().unwrap()(addr, buf, n, direction.into());
        }
    }

    pub fn ref_difftest_regcpy(&self, buf: *mut c_void, direction: DiffertestDirection) {
        unsafe {
            REF_DIFTEST_REGCPY.get().unwrap()(buf, direction.into());
        }
    }

    pub fn ref_difftest_exec(&self, n: u64) {
        unsafe {
            REF_DIFTEST_EXEC.get().unwrap()(n);
        }
    }

    pub fn ref_difftest_raise_intr(&self, n: u64) {
        unsafe {
            REF_DIFTEST_RAISE_INTR.get().unwrap()(n);
        }
    }

    pub fn ref_difftest_init(&self, port: u32) {
        unsafe {
            REF_DIFTEST_INIT.get().unwrap()(port);
        }
    }
}
