use owo_colors::OwoColorize;
pub enum RespType {
    Success,
    Error,
    Warning,
    Info,
    Important,
}

pub fn respstring(tar: &str, msg_type: RespType) -> String {
    match msg_type {
        RespType::Success => format!("{}✅", tar.green()),
        RespType::Error => format!("{}❌", tar.red()),
        RespType::Warning => format!("{}⚠️", tar.yellow()),
        RespType::Info => format!("{}ℹ️", tar.blue()),
        RespType::Important => format!("{}✨", tar.purple()),
    }
}
