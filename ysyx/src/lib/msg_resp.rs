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
        RespType::Trace => format!("{}⏮️", tar),
        RespType::Debug => format!("{}🐞", tar.magenta()),
        RespType::Info => format!("{}ℹ️", tar.blue()),
        RespType::Warning => format!("{}⚠️", tar.yellow()),
        RespType::Error => format!("{}❌", tar.red()),
        RespType::Important => format!("{}✨", tar.purple()),
        RespType::Success => format!("{}✅", tar.green()),
    }
}

pub struct Resper {
    logger: slog::Logger,
}

impl Resper {
    pub fn new() -> Self {
        let log_path = "target/.log";
        let file = OpenOptions::new()
           .create(true)
           .write(true)
           .truncate(true)
           .open(log_path)
           .unwrap();
        
        let decorator = slog_term::PlainDecorator::new(file);
        let drain = slog_term::FullFormat::new(decorator).build().fuse();
        let drain = slog_async::Async::new(drain).build().fuse();

        Self {
            logger: slog::Logger::root(drain, slog::o!()),
        }
    }

    pub fn trace(&self, msg: &str) -> String {
        slog::trace!(self.logger, "{}", msg);
        respstring(msg, RespType::Trace)
    }

    pub fn debug(&self, msg: &str) -> String {
        slog::debug!(self.logger, "{}", msg);
        respstring(msg, RespType::Debug)
    }

    pub fn info(&self, msg: &str) -> String {
        slog::info!(self.logger, "{}", msg);
        respstring(msg, RespType::Info)
    }

    pub fn warning(&self, msg: &str) -> String {
        slog::warn!(self.logger, "{}", msg);
        respstring(msg, RespType::Warning)
    }

    pub fn error(&self, msg: &str) -> String {
        slog::error!(self.logger, "{}", msg);
        respstring(msg, RespType::Error)
    }

    pub fn important(&self, msg: &str) -> String {
        slog::info!(self.logger, "{}", msg);
        respstring(msg, RespType::Important)
    }

    pub fn success(&self, msg: &str) -> String {
        slog::info!(self.logger, "{}", msg);
        respstring(msg, RespType::Success)
    }
}
