use super::super::simulator::Register;
pub struct Riscv32CpuState {
    pub gpr: [Register; 32],
    pub csr: [Register; 4096],
    pub pc: Register,
}

impl Riscv32CpuState {
    pub fn new(reset_vector: u32) -> Self {
        let gpr_name_list = [
            "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
            "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
            "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
            "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
        ];

        Self {
            gpr: core::array::from_fn(|i| Register::new(gpr_name_list[i], 0)),
            csr: core::array::from_fn(|_| Register::new("csr", 0)),
            pc: Register::new("pc", reset_vector),
        }
    }
}