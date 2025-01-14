use crate::simulator;

use super::ExecuteInst;

ysyx_macro::mod_flat!(elf_parser, rv_inst_parser);

pub struct Decoder{
    inst_parser: rv_inst_parser::RvInstParser,
}

impl Decoder{
    pub fn new() -> Self {
        Self {
            inst_parser: rv_inst_parser::RvInstParser::new(),
        }
    }

    pub fn decode(&self, inst: u32) -> Result<ExecuteInst, simulator::SimulatorError> {
        self.inst_parser.parse(inst)
    }
}
