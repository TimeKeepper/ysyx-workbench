#[derive(Debug, PartialEq)]
#[repr(C)]
pub struct Riscv32CpuState {
    pub gpr: [u32; 32],
    pub pc: u32,
    pub csr: [u32; 4096],
}

impl Riscv32CpuState {
    pub fn new(reset_vector: u32) -> Self {
        Self {
            gpr: [0; 32],
            pc: reset_vector,
            csr: [0; 4096],
        }
    }
}