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
