#[cfg(feature = "loongarch32")]
pub struct RegisterBank {
    pub gr: [u32; 32],
    pub pc: u32,
    pub csr: [u32; 1024],
}

#[cfg(feature = "loongarch32")]
impl RegisterBank {
    pub fn new() -> Self {
        RegisterBank {
            gr: [0; 32],
            pc: 0,
            csr: [0; 1024],
        }
    }
}