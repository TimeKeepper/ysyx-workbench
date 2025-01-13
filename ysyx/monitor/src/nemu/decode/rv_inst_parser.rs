use std::ops::Range;

#[derive(Debug, PartialEq, Clone)]
pub struct RiscvInst {
    pub opcode: Range<u8>,
    pub rd: Range<u8>,
    pub funct3: Range<u8>,
    pub rs1: Range<u8>,
    pub rs2: Range<u8>,
    pub funct7: Range<u8>,

    pub name: String,
    pub parser: (u32, u32),
    pub pseudo: Vec<String>,
}

use msg_resp as msgr;

use crate::simulator;

impl RiscvInst {
    pub fn new(name: &str, parser: &str, pseudo: Vec<&str>) -> Self {
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
            pseudo: pseudo.iter().map(|x| x.to_string()).collect(),
        }
    }

    fn parse(&self, input: u32) -> Result<String, Option<()>> {
        if input & self.parser.0 == self.parser.1 {
            return Ok(self.name.to_string());
        }
        Err(None)
    }
}

#[derive(Debug, PartialEq, Clone)]
pub struct RvInstParser {
    insts: Vec<RiscvInst>,
}

impl RvInstParser {
    pub fn parse_pseudo(&self, inst: &str) -> String {
        for i in &self.insts {
            if i.name == inst {
                return inst.to_string();
            } else if i.pseudo.iter().any(|x| x == inst) {
                return i.name.to_string();
            }
        }

        inst.to_string()
    }

    pub fn parse(&self, inst: u32) -> Result<String, simulator::SimulatorError> {
        for i in &self.insts {
            match i.parse(inst) {
                Ok(name) => return Ok(name),
                Err(_) => {},
            }
        }

        Err(simulator::SimulatorError::InstrctionDecodeFailed)
    }

    pub fn new() -> Self {
        let rv32_i_inst = vec![
            RiscvInst::new("lui",       "??????? ????? ????? ??? ????? 01101 11", vec![]),
            RiscvInst::new("auipc",     "??????? ????? ????? ??? ????? 00101 11", vec![]),

            RiscvInst::new("jal",       "??????? ????? ????? ??? ????? 11011 11", vec!["j"]),
            RiscvInst::new("jalr",      "??????? ????? ????? ??? ????? 11001 11", vec!["jr", "ret"]),
            
            RiscvInst::new("beq",       "??????? ????? ????? 000 ????? 11000 11", vec!["beqz"]),
            RiscvInst::new("bne",       "??????? ????? ????? 001 ????? 11000 11", vec!["bnez"]),
            RiscvInst::new("blt",       "??????? ????? ????? 100 ????? 11000 11", vec!["bltz", "bgtz"]),
            RiscvInst::new("bge",       "??????? ????? ????? 101 ????? 11000 11", vec!["blez", "bgez"]),
            RiscvInst::new("bltu",      "??????? ????? ????? 110 ????? 11000 11", vec![]),
            RiscvInst::new("bgeu",      "??????? ????? ????? 111 ????? 11000 11", vec![]),
            
            RiscvInst::new("lb",        "??????? ????? ????? 000 ????? 00000 11", vec![]),
            RiscvInst::new("lh",        "??????? ????? ????? 001 ????? 00000 11", vec![]),
            RiscvInst::new("lw",        "??????? ????? ????? 010 ????? 00000 11", vec![]),
            RiscvInst::new("lbu",       "??????? ????? ????? 100 ????? 00000 11", vec![]),
            RiscvInst::new("lhu",       "??????? ????? ????? 101 ????? 00000 11", vec![]),
            
            RiscvInst::new("sb",        "??????? ????? ????? 000 ????? 01000 11", vec![]),
            RiscvInst::new("sh",        "??????? ????? ????? 001 ????? 01000 11", vec![]),
            RiscvInst::new("sw",        "??????? ????? ????? 010 ????? 01000 11", vec![]),

            RiscvInst::new("addi",      "??????? ????? ????? 000 ????? 00100 11", vec!["nop", "li", "mv"]),

            RiscvInst::new("slti",      "??????? ????? ????? 010 ????? 00100 11", vec![]),
            RiscvInst::new("sltiu",     "??????? ????? ????? 011 ????? 00100 11", vec!["seqz"]),

            RiscvInst::new("xori",      "??????? ????? ????? 100 ????? 00100 11", vec!["not"]),
            RiscvInst::new("ori",       "??????? ????? ????? 110 ????? 00100 11", vec![]),
            RiscvInst::new("andi",      "??????? ????? ????? 111 ????? 00100 11", vec!["zext"]),
            
            RiscvInst::new("slli",      "0000000 ????? ????? 001 ????? 00100 11", vec![]),
            RiscvInst::new("srli",      "0000000 ????? ????? 101 ????? 00100 11", vec![]),
            RiscvInst::new("srai",      "0100000 ????? ????? 101 ????? 00100 11", vec![]),
            
            RiscvInst::new("add",       "0000000 ????? ????? 000 ????? 01100 11", vec![]),
            RiscvInst::new("sub",       "0100000 ????? ????? 000 ????? 01100 11", vec!["neg"]),

            RiscvInst::new("and",       "0000000 ????? ????? 111 ????? 01100 11", vec![]),
            RiscvInst::new("or",        "0000000 ????? ????? 110 ????? 01100 11", vec![]),
            RiscvInst::new("xor",       "0000000 ????? ????? 100 ????? 01100 11", vec![]),

            RiscvInst::new("slt",       "0000000 ????? ????? 010 ????? 01100 11", vec!["sltz", "sgtz"]),
            RiscvInst::new("sltu",      "0000000 ????? ????? 011 ????? 01100 11", vec!["snez"]),
            
            RiscvInst::new("sll",       "0000000 ????? ????? 001 ????? 01100 11", vec![]),
            RiscvInst::new("srl",       "0000000 ????? ????? 101 ????? 01100 11", vec![]),
            RiscvInst::new("sra",       "0100000 ????? ????? 101 ????? 01100 11", vec![]),
            
            RiscvInst::new("fence",     "0000??? ????? 00000 000 00000 00011 11", vec![]),
            RiscvInst::new("ecall",     "0000000 00000 00000 000 00000 11100 11", vec![]),
            RiscvInst::new("ebreak",    "0000000 00001 00000 000 00000 11100 11", vec![]),
        ];

        let zicsr = vec![
            RiscvInst::new("csrrw",     "??????? ????? ????? 001 ????? 11100 11", vec!["csrw"]),
            RiscvInst::new("csrrs",     "??????? ????? ????? 010 ????? 11100 11", vec!["csrr", "csrs"]),
            RiscvInst::new("csrrc",     "??????? ????? ????? 011 ????? 11100 11", vec!["csrc"]),

            RiscvInst::new("csrrwi",    "??????? ????? ????? 101 ????? 11100 11", vec!["csrwi"]),
            RiscvInst::new("csrrsi",    "??????? ????? ????? 110 ????? 11100 11", vec!["csrsi"]),
            RiscvInst::new("csrrci",    "??????? ????? ????? 111 ????? 11100 11", vec!["csrci"]),
        ];

        let r#priv = vec![
            RiscvInst::new("mret",      "0011000 00010 00000 000 00000 11100 11", vec![]),
        ];

        let mut inst = rv32_i_inst;
        inst.extend(zicsr);
        inst.extend(r#priv);

        Self {
            insts: inst,
        }
    }
}

#[cfg(test)]
mod tests {
    use std::io::BufRead;
    use super::super::elf_parser::ElfParser;
    // use crate::executer::nemu::decode::elf_parser::ElfParser;
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

            let parser = RvInstParser::new();
            let data = u32::from_str_radix(&data, 16).unwrap();
            assert_eq!(parser.parse(data), Ok(parser.parse_pseudo(inst)), 
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
        let file= "src/decode/rtthread-riscv32e-ysyxsoc.txt";
        let file = std::fs::File::open(file).unwrap();
        let reader = std::io::BufReader::new(file);
        reader.lines()
    }
}