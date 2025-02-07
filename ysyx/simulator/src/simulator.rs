use owo_colors::OwoColorize;

pub fn function_log(feature: &str, status: bool) {
    println!("{} is [{}]", feature.purple(), if status { "on".green().to_string() } else { "off".red().to_string() });
}

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
