ysyx_macro::mod_pub!(monitor_parser);
ysyx_macro::mod_flat!(init, cmd, monitor);

// pub use msg_resp::{CtrlCommand, ResultMessage, SimErr, SimOk} as msg;

pub mod msg_dependencies {
    pub use msg_resp::{CtrlCommand, ResultMessage, SimErr, SimOk};
}

pub mod state_dependencies {
    pub use state::{
        mmu::devices::{SerialFactory, TimerFactory},
        reg::{RegType, RegisterOps},
        ProcessState,
    };
}
