use slog_term;
use slog_async;
use slog::Drain;

use std::fs::OpenOptions;

use owo_colors::OwoColorize;
pub enum RespType {
    Trace,
    Debug,
    Info,
    Warning,
    Error,
    Important,
    Success,
}

pub fn respstring(tar: &str, msg_type: RespType) -> String {
    match msg_type {
        RespType::Trace => format!("⏮️ {}", tar),
        RespType::Debug => format!("🐞 {}", tar.magenta()),
        RespType::Info => format!("ℹ️ {}", tar.blue()),
        RespType::Warning => format!("⚠️ {}", tar.yellow()),
        RespType::Error => format!("❌ {}", tar.red()),
        RespType::Important => format!("✨ {}", tar.purple()),
        RespType::Success => format!("✅ {}", tar.green()),
    }
}

pub fn print_respstring(tar: &str, msg_type: RespType) {
    println!("{}", respstring(tar, msg_type));
}

pub struct Resper {
    logger: Option<slog::Logger>,
}

impl Resper {
    pub fn new() -> Self {
        Self {
            logger: None,
        }
    }

    pub fn init(&mut self){
        let log_path = "target/.log";

        let file = OpenOptions::new()
           .create(true)
           .write(true)
           .truncate(true)
           .open(log_path)
           .expect("Failed to open log file");

        let decorator = slog_term::PlainDecorator::new(file);
        let drain = slog_term::FullFormat::new(decorator).build().fuse();
        let drain = slog_async::Async::new(drain).build().fuse();

        self.logger = Some(slog::Logger::root(drain, slog::o!()));
    }
    
    fn log(&self, msg: &str) {
        if let Some(logger) = &self.logger {
            slog::trace!(logger, "{}", msg);
        }
    }

    pub fn trace(&self, msg: &str) {
        self.log(msg);
        print_respstring(msg, RespType::Trace);
    }

    pub fn debug(&self, msg: &str) {
        self.log(msg);
        print_respstring(msg, RespType::Debug);
    }

    pub fn info(&self, msg: &str) {
        self.log(msg);
        print_respstring(msg, RespType::Info);
    }

    pub fn warning(&self, msg: &str) {
        self.log(msg);
        print_respstring(msg, RespType::Warning);
    }

    pub fn error(&self, msg: &str) {
        self.log(msg);
        print_respstring(msg, RespType::Error);
    }

    pub fn important(&self, msg: &str) {
        self.log(msg);
        print_respstring(msg, RespType::Important);
    }

    pub fn success(&self, msg: &str) {
        self.log(msg);
        print_respstring(msg, RespType::Success);
    }

    pub fn option_log(&self, fuc: &str, onor_off: bool) {
        if onor_off {
            self.info(&format!("[{}]: {}", fuc.magenta(), "On".green()));
        } else {
            self.info(&format!("[{}]: {}", fuc.magenta(), "Off".red()));
        }
    }
}

#[derive(Debug, PartialEq, Clone)]
pub enum CtrlCommand {
    QUIT,
    FUNC { on_or_off: bool, target: Option<String> },
    DIFFERTEST { path: String, length: u64},
    SI { count: Option<u32> },
    SC { count: Option<u32> },
    T,
}

#[derive(Debug, PartialEq, Clone)]
pub enum SimOk {
    Nothing,
    InstructionExecuted,
    DeviceAttached,
}

#[derive(Debug, PartialEq, Clone)]
pub enum SimErr {
    Signal,
    Ebreak {is_good: bool},
    NotImplemented,
    InvalidCommand,
    InvalidRegIndentifier,
    NoBinaryFile,
    DiffertestFailed,
    BinaryFileNotFound,
    DeviceCannotBeLoad {name: String},
    NoMatchingMemory {msg: MatchMsg},
    NoMatchingDevice {msg: MatchMsg},
    InstrctionDecodeFailed {inst: u32},
    InstrctionExecuteFailed {name: String},
}

pub type ResultMessage = Result<SimOk, SimErr>;

#[derive(Debug, PartialEq, Clone)]
pub enum MatchMsg {
    ADDR {addr: u32},
    NAME {name: String},
}

use std::fmt::Display;
impl Display for MatchMsg {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            MatchMsg::ADDR {addr} => write!(f, "ADDR: 0x{:08x}", addr),
            MatchMsg::NAME {name} => write!(f, "NAME: {}", name),
        }
    }
}
