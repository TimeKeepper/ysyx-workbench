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
    pub fn new(log_path: Option<String>) -> Self {

        if log_path.is_none() {
            print_respstring("Log file no specified", RespType::Warning);
            return Self { logger: None };
        }

        print_respstring("Log file specified", RespType::Info);

        let file = OpenOptions::new()
           .create(true)
           .write(true)
           .truncate(true)
           .open(log_path.unwrap())
           .unwrap();
        
        let decorator = slog_term::PlainDecorator::new(file);
        let drain = slog_term::FullFormat::new(decorator).build().fuse();
        let drain = slog_async::Async::new(drain).build().fuse();

        Self {
            logger: Some(slog::Logger::root(drain, slog::o!())),
        }
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

    pub fn function_log(&self, fuc: &str, onor_off: bool) {
        if onor_off {
            self.info(&format!("[{}]: {}", fuc.magenta(), "On".green()));
        } else {
            self.info(&format!("[{}]: {}", fuc.magenta(), "Off".red()));
        }
    }
}
