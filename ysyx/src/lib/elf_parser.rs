use winnow::token::take_while;
use winnow::{PResult, Parser};

pub struct ElfParser{
    vir_inst : Vec<(String, String)>
}

impl ElfParser{
    pub fn new() -> Self {
        Self {
            vir_inst: vec![
                ("nop".to_string(),     "addi".to_string()),

                ("neg".to_string(),     "sub".to_string()),

                ("snez".to_string(),    "sltu".to_string()),
                ("seqz".to_string(),    "sltiu".to_string()),
                ("sltz".to_string(),    "slt".to_string()),
                ("sgtz".to_string(),    "slt".to_string()),

                ("beqz".to_string(),    "beq".to_string()),
                ("bnez".to_string(),    "bne".to_string()),
                ("blez".to_string(),    "bge".to_string()),
                ("bgez".to_string(),    "bge".to_string()),
                ("bltz".to_string(),    "blt".to_string()),
                ("bgtz".to_string(),    "blt".to_string()),
                
                ("j".to_string(),       "jal".to_string()),
                ("jr".to_string(),      "jalr".to_string()),
                ("ret".to_string(),     "jalr".to_string()),

                ("li".to_string(),      "addi".to_string()),
                ("mv".to_string(),      "addi".to_string()),

                ("csrr".to_string(),    "csrrs".to_string()),
                ("csrw".to_string(),    "csrrw".to_string()),
                ("csrs".to_string(),    "csrrs".to_string()),
                ("csrc".to_string(),    "csrrc".to_string()),

                ("zext".to_string(),    "andi".to_string()),

                ("not".to_string(),     "xori".to_string()),
            ],
        }
    }

    fn parse_addr<'s>(input: &mut &'s str) -> PResult<&'s str> {
        take_while(1.., ('0'..='9', 'a'..='f', ' ')).parse_next(input)
    }
    
    fn parse_midfix<'s>(input: &mut &'s str) -> PResult<&'s str> {
        ":	".parse_next(input)
    }
    
    fn parse_data<'s>(input: &mut &'s str) -> PResult<&'s str> {
        take_while(1.., ('0'..='9', 'a'..='f')).parse_next(input)
    }
    
    fn parse_finalfix<'s>(input: &mut &'s str) -> PResult<&'s str> {
        "          	".parse_next(input)
    }
    
    fn parse_inst<'s>(input: &mut &'s str) -> PResult<&'s str> {
        take_while(1.., ('0'..='9', 'a'..='z', 'A'..='Z', '\t', '+', '-', ',')).parse_next(input)
    }
    
    pub fn parse<'s>(&mut self, input: &mut &'s str) -> PResult<(&'s str, &'s str, &'s str)> {
        let addr = ElfParser::parse_addr(input)?;
        let _ = ElfParser::parse_midfix(input)?;
        let data = ElfParser::parse_data(input)?;
        let _ = ElfParser::parse_finalfix(input)?;
        let inst = ElfParser::parse_inst(input)?;
    
        Ok((addr, data, inst))
    }
    
    pub fn parser_inst_name<'s>(input: &mut &'s str) -> PResult<&'s str> {
        take_while(1.., 'a'..='z').parse_next(input)
    }

    pub fn vir2phy_inst(&mut self, vir_inst: &str) -> String{
        for i in &self.vir_inst {
            if i.0 == vir_inst{
                return i.1.to_string();
            }
        }
        return vir_inst.to_string();
    }
}