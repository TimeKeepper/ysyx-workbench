use winnow::token::take_while;
use winnow::{ModalResult, Parser};

pub struct ElfParser;

impl ElfParser {
    pub fn new() -> Self {
        Self
    }

    fn parse_addr<'s>(input: &mut &'s str) -> ModalResult<&'s str> {
        take_while(1.., ('0'..='9', 'a'..='f', ' ')).parse_next(input)
    }

    fn parse_midfix<'s>(input: &mut &'s str) -> ModalResult<&'s str> {
        ":	".parse_next(input)
    }

    fn parse_data<'s>(input: &mut &'s str) -> ModalResult<&'s str> {
        take_while(1.., ('0'..='9', 'a'..='f')).parse_next(input)
    }

    fn parse_finalfix<'s>(input: &mut &'s str) -> ModalResult<&'s str> {
        "          	".parse_next(input)
    }

    fn parse_inst<'s>(input: &mut &'s str) -> ModalResult<&'s str> {
        take_while(1.., ('0'..='9', 'a'..='z', 'A'..='Z', '\t', '+', '-', ',')).parse_next(input)
    }

    pub fn parse<'s>(&mut self, input: &mut &'s str) -> ModalResult<(&'s str, &'s str, &'s str)> {
        let addr = ElfParser::parse_addr(input)?;
        let _ = ElfParser::parse_midfix(input)?;
        let data = ElfParser::parse_data(input)?;
        let _ = ElfParser::parse_finalfix(input)?;
        let inst = ElfParser::parse_inst(input)?;

        Ok((addr, data, inst))
    }

    pub fn parser_inst_name<'s>(input: &mut &'s str) -> ModalResult<&'s str> {
        take_while(1.., 'a'..='z').parse_next(input)
    }
}
