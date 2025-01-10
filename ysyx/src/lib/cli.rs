use clap::Parser;

/// CLI options for nemu / npc
#[derive(Parser)]
#[command(version, about, long_about = None)]
struct Cli {
    /// Batch mode
    #[arg(short, long)]
    pub batch: bool,

    /// Log file path
    #[arg(short, long)]
    pub log: Option<String>,

    /// differtest dut file
    #[arg(short, long)]
    pub dut: Option<String>,

    /// ELF file path
    #[arg(short, long)]
    pub elf: Option<String>,
}

pub fn parser() -> (bool, Option<String>, Option<String>, Option<String>) {
    let parse = Cli::parse();
    (parse.batch, parse.log, parse.dut, parse.elf)
}
