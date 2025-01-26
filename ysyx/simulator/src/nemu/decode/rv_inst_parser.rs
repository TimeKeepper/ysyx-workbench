#[derive(Debug, PartialEq, Clone)]
enum ImmType {
    I,
    S,
    B,
    U,
    J,
    R,
    N,
}

#[derive(Debug, PartialEq, Clone)]
struct RiscvInst {
    pub name: String,
    pub imm_type: ImmType,
    pub parser: (u32, u32),
    pub pseudo: Vec<String>,
}

use msg_resp as msgr;

use crate::{nemu::ExecuteInst, simulator};

use super::super::{extract_bits, sig_extend};

impl RiscvInst {
    pub fn new(name: &str, imm_type: ImmType, parser: &str, pseudo: Vec<&str>) -> Self {
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
            name: name.to_string(),
            imm_type,
            parser: (!mask, key),
            pseudo: pseudo.iter().map(|x| x.to_string()).collect(),
        }
    }

    fn get_imm(&self, input: u32) -> u32 {
        match self.imm_type {
            ImmType::I => {
                let range = 20..31;
                let imm = extract_bits(input, range.clone());
                sig_extend(imm, range.end as u8 - range.start as u8 + 1)
            },
            ImmType::S => {
                // println!("input: {:032b}", input);
                // println!("{:07b}", extract_bits(input, 25..31));
                // println!("{:06b}", extract_bits(input, 7..11));
                let imm = (extract_bits(input, 25..31) << 5) | extract_bits(input, 7..11);
                sig_extend(imm, 12)
            },
            ImmType::B => {
                let imm = (extract_bits(input, 31..31) << 12) | (extract_bits(input, 25..30) << 5) | (extract_bits(input, 8..11) << 1) | (extract_bits(input, 7..7) << 11);
                sig_extend(imm, 13)
            },
            ImmType::U => {
                extract_bits(input, 12..31) << 12
            },
            ImmType::J => {
                let imm = (extract_bits(input, 31..31) << 20) | (extract_bits(input, 12..19) << 12) | (extract_bits(input, 20..20) << 11) | (extract_bits(input, 21..30) << 1);
                sig_extend(imm, 21)
            },
            ImmType::R => {
                0
            },
            ImmType::N => {
                0
            },
        }
    }

    fn parse(&self, input: u32) -> Result<ExecuteInst, Option<()>> {
        if input & self.parser.0 == self.parser.1 {
            return Ok(ExecuteInst::new(
                &self.name,
                extract_bits(input, 15..19) as u8,
                extract_bits(input, 20..24) as u8,
                extract_bits(input, 7..11) as u8,
                self.get_imm(input),
            ));
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

    pub fn parse(&self, inst: u32) -> Result<ExecuteInst, simulator::SimulatorError> {
        for i in &self.insts {
            match i.parse(inst) {
                Ok(name) => return Ok(name),
                Err(_) => {},
            }
        }
        
        Err(simulator::SimulatorError::InstrctionDecodeFailed{inst})
    }

    pub fn new() -> Self {
        let rv32_i_inst = vec![
            RiscvInst::new("lui",       ImmType::U, "??????? ????? ????? ??? ????? 01101 11", vec![]),
            RiscvInst::new("auipc",     ImmType::U, "??????? ????? ????? ??? ????? 00101 11", vec![]),

            RiscvInst::new("jal",       ImmType::J, "??????? ????? ????? ??? ????? 11011 11", vec!["j"]),
            RiscvInst::new("jalr",      ImmType::I, "??????? ????? ????? ??? ????? 11001 11", vec!["jr", "ret"]),
            
            RiscvInst::new("beq",       ImmType::B, "??????? ????? ????? 000 ????? 11000 11", vec!["beqz"]),
            RiscvInst::new("bne",       ImmType::B, "??????? ????? ????? 001 ????? 11000 11", vec!["bnez"]),
            RiscvInst::new("blt",       ImmType::B, "??????? ????? ????? 100 ????? 11000 11", vec!["bltz", "bgtz"]),
            RiscvInst::new("bge",       ImmType::B, "??????? ????? ????? 101 ????? 11000 11", vec!["blez", "bgez"]),
            RiscvInst::new("bltu",      ImmType::B, "??????? ????? ????? 110 ????? 11000 11", vec![]),
            RiscvInst::new("bgeu",      ImmType::B, "??????? ????? ????? 111 ????? 11000 11", vec![]),
            
            RiscvInst::new("lb",        ImmType::I, "??????? ????? ????? 000 ????? 00000 11", vec![]),
            RiscvInst::new("lh",        ImmType::I, "??????? ????? ????? 001 ????? 00000 11", vec![]),
            RiscvInst::new("lw",        ImmType::I, "??????? ????? ????? 010 ????? 00000 11", vec![]),
            RiscvInst::new("lbu",       ImmType::I, "??????? ????? ????? 100 ????? 00000 11", vec![]),
            RiscvInst::new("lhu",       ImmType::I, "??????? ????? ????? 101 ????? 00000 11", vec![]),
            
            RiscvInst::new("sb",        ImmType::S, "??????? ????? ????? 000 ????? 01000 11", vec![]),
            RiscvInst::new("sh",        ImmType::S, "??????? ????? ????? 001 ????? 01000 11", vec![]),
            RiscvInst::new("sw",        ImmType::S, "??????? ????? ????? 010 ????? 01000 11", vec![]),

            RiscvInst::new("addi",      ImmType::I, "??????? ????? ????? 000 ????? 00100 11", vec!["nop", "li", "mv"]),

            RiscvInst::new("slti",      ImmType::I, "??????? ????? ????? 010 ????? 00100 11", vec![]),
            RiscvInst::new("sltiu",     ImmType::I, "??????? ????? ????? 011 ????? 00100 11", vec!["seqz"]),

            RiscvInst::new("xori",      ImmType::I, "??????? ????? ????? 100 ????? 00100 11", vec!["not"]),
            RiscvInst::new("ori",       ImmType::I, "??????? ????? ????? 110 ????? 00100 11", vec![]),
            RiscvInst::new("andi",      ImmType::I, "??????? ????? ????? 111 ????? 00100 11", vec!["zext"]),
            
            RiscvInst::new("slli",      ImmType::I, "0000000 ????? ????? 001 ????? 00100 11", vec![]),
            RiscvInst::new("srli",      ImmType::I, "0000000 ????? ????? 101 ????? 00100 11", vec![]),
            RiscvInst::new("srai",      ImmType::I, "0100000 ????? ????? 101 ????? 00100 11", vec![]),
            
            RiscvInst::new("add",       ImmType::R, "0000000 ????? ????? 000 ????? 01100 11", vec![]),
            RiscvInst::new("sub",       ImmType::R, "0100000 ????? ????? 000 ????? 01100 11", vec!["neg"]),

            RiscvInst::new("xor",       ImmType::R, "0000000 ????? ????? 100 ????? 01100 11", vec![]),
            RiscvInst::new("or",        ImmType::R, "0000000 ????? ????? 110 ????? 01100 11", vec![]),
            RiscvInst::new("and",       ImmType::R, "0000000 ????? ????? 111 ????? 01100 11", vec![]),

            RiscvInst::new("slt",       ImmType::R, "0000000 ????? ????? 010 ????? 01100 11", vec!["sltz", "sgtz"]),
            RiscvInst::new("sltu",      ImmType::R, "0000000 ????? ????? 011 ????? 01100 11", vec!["snez"]),
            
            RiscvInst::new("sll",       ImmType::R, "0000000 ????? ????? 001 ????? 01100 11", vec![]),
            RiscvInst::new("srl",       ImmType::R, "0000000 ????? ????? 101 ????? 01100 11", vec![]),
            RiscvInst::new("sra",       ImmType::R, "0100000 ????? ????? 101 ????? 01100 11", vec![]),
            
            RiscvInst::new("fence",     ImmType::N, "0000??? ????? 00000 000 00000 00011 11", vec![]),
            RiscvInst::new("ecall",     ImmType::N, "0000000 00000 00000 000 00000 11100 11", vec![]),
            RiscvInst::new("ebreak",    ImmType::N, "0000000 00001 00000 000 00000 11100 11", vec![]),
        ];

        let rv32_m_inst = vec![
            RiscvInst::new("mul",       ImmType::R, "0000001 ????? ????? 000 ????? 01100 11", vec![]),

            RiscvInst::new("mulh",      ImmType::R, "0000001 ????? ????? 001 ????? 01100 11", vec![]),
            RiscvInst::new("mulhsu",    ImmType::R, "0000001 ????? ????? 010 ????? 01100 11", vec![]),
            RiscvInst::new("mulhu",     ImmType::R, "0000001 ????? ????? 011 ????? 01100 11", vec![]),

            RiscvInst::new("div",       ImmType::R, "0000001 ????? ????? 100 ????? 01100 11", vec![]),
            RiscvInst::new("divu",      ImmType::R, "0000001 ????? ????? 101 ????? 01100 11", vec![]),

            RiscvInst::new("rem",       ImmType::R, "0000001 ????? ????? 110 ????? 01100 11", vec![]),
            RiscvInst::new("remu",      ImmType::R, "0000001 ????? ????? 111 ????? 01100 11", vec![]),
        ];

        let zicsr = vec![
            RiscvInst::new("csrrw",     ImmType::I, "??????? ????? ????? 001 ????? 11100 11", vec!["csrw"]),
            RiscvInst::new("csrrs",     ImmType::I, "??????? ????? ????? 010 ????? 11100 11", vec!["csrr", "csrs"]),
            RiscvInst::new("csrrc",     ImmType::I, "??????? ????? ????? 011 ????? 11100 11", vec!["csrc"]),

            RiscvInst::new("csrrwi",    ImmType::U, "??????? ????? ????? 101 ????? 11100 11", vec!["csrwi"]),
            RiscvInst::new("csrrsi",    ImmType::U, "??????? ????? ????? 110 ????? 11100 11", vec!["csrsi"]),
            RiscvInst::new("csrrci",    ImmType::U, "??????? ????? ????? 111 ????? 11100 11", vec!["csrci"]),
        ];

        let r#priv = vec![
            RiscvInst::new("mret",      ImmType::N, "0011000 00010 00000 000 00000 11100 11", vec![]),
        ];

        let mut inst = rv32_i_inst;
        inst.extend(rv32_m_inst);
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
            assert_eq!(parser.parse(data).unwrap().name, parser.parse_pseudo(inst), 
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
        let file= "src/test/rtthread-riscv32e-ysyxsoc.txt";
        let file = std::fs::File::open(file).unwrap();
        let reader = std::io::BufReader::new(file);
        reader.lines()
    }
}