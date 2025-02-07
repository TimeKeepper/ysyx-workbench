use clap::Parser;

/// CLI options for nemu / npc
#[derive(Parser)]
#[command(version, about, long_about = None)]
pub struct Cli {
    /// Batch mode
    #[arg(short, long)]
    pub batch: bool,

    /// Debug mode
    #[arg(long)]
    pub debug: bool,

    /// differtest dut file
    #[arg(short, long)]
    pub dut: Option<String>,

    /// bin file path
    #[arg(long)]
    pub bin: Option<String>,

    /// ELF file path
    #[arg(short, long)]
    pub elf: Option<String>,
}
