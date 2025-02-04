use owo_colors::OwoColorize;

pub fn function_log(feature: &str, status: bool) {
    println!("{} is [{}]", feature.purple(), if status { "on".green().to_string() } else { "off".red().to_string() });
}

// pub trait Simulator {
//     fn single_instruction(&mut self, trace: bool) -> ResultMessage;

//     fn instruction_ring_buffer(&mut self);

//     fn times(&mut self);

//     fn external_state_change(&mut self) -> bool;

//     fn get_reg_state(&self) -> &Riscv32CpuState;

//     fn get_mem_state(&mut self) -> &mut MMU;

//     fn state(&mut self, target: String, specify: Option<String>) -> ResultMessage;

//     fn func_ctrl(&mut self, on_or_off: bool, target: Option<&str>) -> ResultMessage;
// }

#[derive(Debug, PartialEq, Clone)]
pub struct Register {
    pub name: &'static str,
    pub value: u32,
}

impl Register {
    pub fn new(name: &'static str, value: u32) -> Self {
        Self {
            name,
            value,
        }
    }
}
