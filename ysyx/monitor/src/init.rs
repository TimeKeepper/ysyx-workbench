use msg_resp::{CtrlCommand, SimErr, SimOk};
use state::{
    mmu::devices::{SerialFactory, TimerFactory},
    reg::RegisterOps,
    ProcessState,
};
use ysyx_macro::with_rwlock_write;

use crate::Monitor;

impl Monitor {
    pub fn init(&mut self) {
        self.init_mem();

        self.init_signal();

        self.init_log();

        self.init_sim();

        if self.cli_parser.elf.is_some() {
            self.resper
                .lock()
                .unwrap()
                .error("ELF file path is not implemented yet");
        }

        if self.cli_parser.debug {
            self.cmd_sender
                .send(CtrlCommand::FUNC {
                    on_or_off: Some(true),
                    target: None,
                })
                .unwrap();
            assert!(matches!(
                self.result_receiver.recv().unwrap(),
                Ok(SimOk::FunctionCtrl)
            ));
        }
    }

    fn init_mem(&mut self) {
        with_rwlock_write!(self.mem, mem, {
            mem.add_memory("sram", 0x0f00_0000, 0x0000_2000);
            mem.add_memory("mrom", 0x2000_0000, 0x0000_1000);
            mem.add_memory("flash", 0x3000_0000, 0x1000_0000);
            mem.add_memory("psram", 0x8000_0000, 0x0800_0000);
            mem.add_memory("sdram", 0xa000_0000, 0x0200_0000);

            mem.add_device(SerialFactory::new(0x1000_0000));
            mem.add_device(TimerFactory::new(0x1000_2000));
        });
    }

    fn init_signal(&mut self) {
        let resper = self.resper.clone();
        let state = self.state.clone();
        ctrlc::set_handler(move || {
            resper.lock().unwrap().info("Ctrl-C received");
            *state.write().unwrap() = ProcessState::STOP;
        })
        .expect("Error setting Ctrl-C handler");
    }

    fn init_log(&mut self) {
        #[cfg(feature = "log")]
        self.resper.lock().unwrap().init();

        self.resper
            .lock()
            .unwrap()
            .option_log("log", cfg!(feature = "log"));
    }

    fn init_sim(&mut self) {
        with_rwlock_write!(self.reg, reg, {
            reg.write_pc(0x8000_0000);
        });

        if self.cli_parser.bin.is_none() {
            self.resper.lock().unwrap().error("No binary file");
            return;
        }
        let bin = std::fs::read(self.cli_parser.bin.clone().unwrap());
        if bin.is_err() {
            self.resper.lock().unwrap().error("Binary file not found");
            *self.state.write().unwrap() = ProcessState::ABORT;
            return;
        }
        let bin: Vec<u8> = bin.unwrap();

        with_rwlock_write!(self.mem, mem, {
            let _ = mem.load("psram", &bin);
        });

        if let Some(diffpath) = &self.cli_parser.dut {
            self.cmd_sender
                .send(CtrlCommand::DIFFERTEST {
                    path: diffpath.clone(),
                    length: bin.len() as u64,
                })
                .unwrap();
            assert!(matches!(self.result_receiver.recv().unwrap(), Ok(SimOk::DiffertestInit) | Err(SimErr::DiffertestFailedToInit)));
        }
    }
}
