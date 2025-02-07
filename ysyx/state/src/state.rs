#[derive(Debug, PartialEq, Clone)]
pub enum ProcessState {
    RUNNING,
    STOP,

    // Done state
    DONE,
    TRAP,

    // Quit state
    QUIT,
    ABORT,
}

impl ProcessState {
    pub fn new() -> Self {
        ProcessState::STOP
    }

    pub fn is_run(&self) -> bool {
        matches!(self, ProcessState::RUNNING)
    }

    pub fn set(&mut self, state: ProcessState) {
        *self = state;
    }
}
