use std::ops::Range;

#[derive(Debug,PartialEq)]
pub struct RiscvInst {
    pub opcode: Range<u8>,
    pub rd: Range<u8>,
    pub funct3: Range<u8>,
    pub rs1: Range<u8>,
    pub rs2: Range<u8>,
    pub funct7: Range<u8>,

    pub name: String,
    pub parser: (u32, u32),
}

use crate::msg_resp as msgr;

impl RiscvInst {
    pub fn new(name: &str, parser: &str) -> Self {
        let parser = parser.replace(" ", "");

        assert_eq!(parser.len(), 32, 
            "{}", msgr::respstring("Invalid length of parser string", msgr::RespType::Error));

        for char in parser.chars() {
            assert!(char == '0' || char == '1' || char == '?', 
                "{}", msgr::respstring("Invalid character in parser string", msgr::RespType::Error));
        }

        let mut mask = 0;
        let mut key = 0;
        for i in parser.chars() {
            mask <<= 1;
            key <<= 1;
            if i == '?' {
                mask |= 1;
            } else if i == '1' {
                key |= 1;
            }
        }

        Self {
            opcode: 0..7,
            rd: 7..12,
            funct3: 12..15,
            rs1: 15..20,
            rs2: 20..25,
            funct7: 25..32,
            name: name.to_string(),
            parser: (!mask, key),
        }
    }

    fn parse(&self, input: u32) -> Result<String, Option<()>> {
        if input & self.parser.0 == self.parser.1 {
            return Ok(self.name.to_string());
        }
        Err(None)
    }
}

pub struct RvInstParser {
    insts: Vec<RiscvInst>,
}

impl RvInstParser {
    pub fn new() -> Self {
        Self {
            insts: vec![
                RiscvInst::new("lui",       "??????? ????? ????? ??? ????? 01101 11"),
                RiscvInst::new("auipc",     "??????? ????? ????? ??? ????? 00101 11"),

                RiscvInst::new("jal",       "??????? ????? ????? ??? ????? 11011 11"),
                RiscvInst::new("jalr",      "??????? ????? ????? ??? ????? 11001 11"),
                
                RiscvInst::new("sb",        "??????? ????? ????? 000 ????? 01000 11"),
                RiscvInst::new("sh",        "??????? ????? ????? 001 ????? 01000 11"),
                RiscvInst::new("sw",        "??????? ????? ????? 010 ????? 01000 11"),
                
                RiscvInst::new("lb",        "??????? ????? ????? 000 ????? 00000 11"),
                RiscvInst::new("lbu",       "??????? ????? ????? 100 ????? 00000 11"),
                RiscvInst::new("lh",        "??????? ????? ????? 001 ????? 00000 11"),
                RiscvInst::new("lhu",       "??????? ????? ????? 101 ????? 00000 11"),
                RiscvInst::new("lw",        "??????? ????? ????? 010 ????? 00000 11"),
                
                RiscvInst::new("add",       "0000000 ????? ????? 000 ????? 01100 11"),
                RiscvInst::new("sub",       "0100000 ????? ????? 000 ????? 01100 11"),
                RiscvInst::new("and",       "0000000 ????? ????? 111 ????? 01100 11"),
                RiscvInst::new("or",        "0000000 ????? ????? 110 ????? 01100 11"),
                RiscvInst::new("xor",       "0000000 ????? ????? 100 ????? 01100 11"),

                RiscvInst::new("addi",      "??????? ????? ????? 000 ????? 00100 11"),
                RiscvInst::new("andi",      "??????? ????? ????? 111 ????? 00100 11"),
                RiscvInst::new("ori",       "??????? ????? ????? 110 ????? 00100 11"),
                RiscvInst::new("xori",      "??????? ????? ????? 100 ????? 00100 11"),

                RiscvInst::new("slti",      "??????? ????? ????? 010 ????? 00100 11"),
                RiscvInst::new("sltiu",     "??????? ????? ????? 011 ????? 00100 11"),

                RiscvInst::new("slt",       "0000000 ????? ????? 010 ????? 01100 11"),
                RiscvInst::new("sltu",      "0000000 ????? ????? 011 ????? 01100 11"),
                
                RiscvInst::new("sll",       "0000000 ????? ????? 001 ????? 01100 11"),
                RiscvInst::new("srl",       "0000000 ????? ????? 101 ????? 01100 11"),
                RiscvInst::new("sra",       "0100000 ????? ????? 101 ????? 01100 11"),
                
                RiscvInst::new("slli",      "0000000 ????? ????? 001 ????? 00100 11"),
                RiscvInst::new("srli",      "0000000 ????? ????? 101 ????? 00100 11"),
                RiscvInst::new("srai",      "0100000 ????? ????? 101 ????? 00100 11"),
                
                RiscvInst::new("beq",       "??????? ????? ????? 000 ????? 11000 11"),
                RiscvInst::new("bne",       "??????? ????? ????? 001 ????? 11000 11"),
                RiscvInst::new("blt",       "??????? ????? ????? 100 ????? 11000 11"),
                RiscvInst::new("bge",       "??????? ????? ????? 101 ????? 11000 11"),
                RiscvInst::new("bltu",      "??????? ????? ????? 110 ????? 11000 11"),
                RiscvInst::new("bgeu",      "??????? ????? ????? 111 ????? 11000 11"),
                
                RiscvInst::new("csrrw",     "??????? ????? ????? 001 ????? 11100 11"),
                RiscvInst::new("csrrs",     "??????? ????? ????? 010 ????? 11100 11"),
                
                RiscvInst::new("ecall",     "0000000 00000 00000 000 00000 11100 11"),
                RiscvInst::new("ebreak",    "0000000 00001 00000 000 00000 11100 11"),
                RiscvInst::new("mret",      "0011000 00010 00000 000 00000 11100 11"),
            ],
        }
    }

    pub fn add_inst(&mut self, inst: RiscvInst) {
        self.insts.push(inst);
    }

    pub fn parse(&self, inst: u32) -> Result<String, String> {
        for i in &self.insts {
            match i.parse(inst) {
                Ok(name) => return Ok(name),
                Err(_) => {},
            }
        }

        Err(msgr::respstring("Invalid instruction", msgr::RespType::Error))
    }
}

#[cfg(test)]
mod tests {
    use std::io::BufRead;
    use crate::elf_parser::ElfParser;
    use super::*;

    #[test]
    fn decode_test() {
        let lines = get_testfile();
        let mut elf_parser = ElfParser::new();

        for line in lines {
            let line_str = line.unwrap();
            let mut last_line_str = line_str.as_str();
            let result = elf_parser.parse(&mut last_line_str);
            if result.is_err() {
                // println!("Failed to parse line: {}", line_str);
                continue;
            }
            let (_, data, mut inst) = result.unwrap();
            inst = ElfParser::parser_inst_name(&mut inst).unwrap();
            let phy_inst = elf_parser.vir2phy_inst(&inst.to_string());
            inst = &phy_inst;

            let parser = RvInstParser::new();
            let data = u32::from_str_radix(&data, 16).unwrap();
            assert_eq!(parser.parse(data), Ok(inst.to_string()), 
                "{}-lines:{}", msgr::respstring("Failed to decode instruction", msgr::RespType::Error), line_str);
        }
    }

    #[test]
    fn get_testbench() {
        let lines = get_testfile();
        let mut elf_parser = ElfParser::new();
    
        for line in lines {
            let line_str = line.unwrap();
            let mut last_line_str = line_str.as_str();
            let result = elf_parser.parse(&mut last_line_str);
            if result.is_err() {
                // println!("Failed to parse line: {}", line_str);
                continue;
            }
            let (addr, data, mut inst) = result.unwrap();
            println!("addr: {}, data: {}, inst: {}", addr, data, ElfParser::parser_inst_name(&mut inst).unwrap());
        }
    }

    fn get_testfile() -> std::io::Lines<std::io::BufReader<std::fs::File>> {
        let file= "src/lib/rtthread-riscv32e-ysyxsoc.txt";
        let file = std::fs::File::open(file).unwrap();
        let reader = std::io::BufReader::new(file);
        reader.lines()
    }
}