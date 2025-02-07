use std::ffi::c_int;

use const_cstr::const_cstr;
use dlopen2::{raw::Library, symbor::SymBorApi, wrapper::WrapperApi};

type SramRead = extern "C" fn(u32, *mut u32);
type SramWrite = extern "C" fn(u32, u32, u32);
type IfuCatch = extern "C" fn(u32);
type IcacheCatch = extern "C" fn(u32, u32);
type IduCatch = extern "C" fn(u32);
type AluCatch = extern "C" fn();
type LsuCatch = extern "C" fn();
type WbuCatch = extern "C" fn(u32, u32, u32, u32, u32, u32, u32, u32, u32);

pub struct NpcWrapper {
    lib: Library,
    
    tick: fn(),
    single_cycle: fn(),
    reset: fn(u32),
    quit: fn(),
}

impl NpcWrapper {
    pub fn new(path: &str) -> Self {
        let lib = Library::open(path)
            .expect("Failed to open library");
        
        let tick: fn() = unsafe { lib.symbol_cstr::<fn()>(const_cstr!("tick").as_cstr()) }
            .unwrap();

        let single_cycle: fn() = unsafe { lib.symbol_cstr::<fn()>(const_cstr!("single_cycle").as_cstr()) }
            .unwrap();

        let reset: fn(u32) = unsafe { lib.symbol_cstr::<fn(u32)>(const_cstr!("reset").as_cstr()) }
            .unwrap();

        let quit: fn() = unsafe { lib.symbol_cstr::<fn()>(const_cstr!("quit").as_cstr()) }
            .unwrap();

        Self { 
            lib,

            tick,
            single_cycle,
            reset,
            quit,
        }
    }

    pub fn init_sram_read(&self, callback: SramRead) {
        let init_sram_read =
            unsafe { self.lib.symbol_cstr::<fn(SramRead)>(const_cstr!("init_sram_read").as_cstr()) }
                .unwrap();
        init_sram_read(callback);
    }

    pub fn init_sram_write(&self, callback: SramWrite) {
        let init_sram_write =
            unsafe { self.lib.symbol_cstr::<fn(SramWrite)>(const_cstr!("init_sram_write").as_cstr()) }
                .unwrap();
        init_sram_write(callback);
    }

    pub fn init_ifu_catch(&self, callback: IfuCatch) {
        let init_ifu_catch =
            unsafe { self.lib.symbol_cstr::<fn(IfuCatch)>(const_cstr!("init_IFU_catch").as_cstr()) }
                .unwrap();
        init_ifu_catch(callback);
    }

    pub fn init_icache_catch(&self, callback: IcacheCatch) {
        let init_icache_catch =
            unsafe { self.lib.symbol_cstr::<fn(IcacheCatch)>(const_cstr!("init_Icache_catch").as_cstr()) }
                .unwrap();
        init_icache_catch(callback);
    }

    pub fn init_idu_catch(&self, callback: IduCatch) {
        let init_idu_catch =
            unsafe { self.lib.symbol_cstr::<fn(IduCatch)>(const_cstr!("init_IDU_catch").as_cstr()) }
                .unwrap();
        init_idu_catch(callback);
    }

    pub fn init_alu_catch(&self, callback: AluCatch) {
        let init_alu_catch =
            unsafe { self.lib.symbol_cstr::<fn(AluCatch)>(const_cstr!("init_ALU_catch").as_cstr()) }
                .unwrap();
        init_alu_catch(callback);
    }

    pub fn init_lsu_catch(&self, callback: LsuCatch) {
        let init_lsu_catch =
            unsafe { self.lib.symbol_cstr::<fn(LsuCatch)>(const_cstr!("init_LSU_catch").as_cstr()) }
                .unwrap();
        init_lsu_catch(callback);
    }

    pub fn init_wbu_catch(&self, callback: WbuCatch) {
        let init_wbu_catch =
            unsafe { self.lib.symbol_cstr::<fn(WbuCatch)>(const_cstr!("init_WBU_catch").as_cstr()) }
                .unwrap();
        init_wbu_catch(callback);
    }

    pub fn init(&self, argc: i32, argv: *const *const u8) {
        let init: fn(i32, *const *const u8) = 
            unsafe { self.lib.symbol_cstr::<fn(c_int, *const *const u8)>(const_cstr!("init").as_cstr()) }
            .unwrap();
        init(argc, argv);
    }

    pub fn tick(&self) {
        (self.tick)();
    }

    pub fn single_cycle(&self) {
        (self.single_cycle)();
    }

    pub fn reset(&self, pc: u32) {
        (self.reset)(pc);
    }

    pub fn quit(&self) {
        (self.quit)();
    }
}
