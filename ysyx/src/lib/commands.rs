use crate::msg_resp as msgr;
use clap::{Parser, Subcommand};

#[derive(Parser, Debug)]
#[command(author, version, about)]
pub struct Command {
    #[command(subcommand)]
    command: Commands,
}

#[derive(Debug, Subcommand)]
#[command(author, version, about)]
pub enum Commands {
    /// does testing things
    #[clap(visible_alias = "t")]
    Test {
        /// lists test values
        #[arg(short, long)]
        list: bool,
    },

    /// quit the program, not need any arguments
    #[clap(visible_alias = "q")]
    Quit {},
}

use owo_colors::OwoColorize;
pub struct CommandManager {
    name: String,
    rl: rustyline::DefaultEditor,
}

use rustyline::DefaultEditor;
impl CommandManager {
    pub fn get_parser(&mut self) -> Commands {
        loop {
            let input = self.rl_get();

            let mut input: Vec<&str> = input.trim().split_whitespace().collect::<Vec<&str>>();
            if input.is_empty() {
                continue;
            }

            input.insert(0, ""); // Insert a dummy value

            let cli = Command::try_parse_from(input);

            if cli.is_ok() {
                return cli.unwrap().command;
            }

            let cli = cli.unwrap_err();

            if cli.kind() == clap::error::ErrorKind::DisplayHelp {
                println!("{}", cli.green());
                continue;
            }

            println!(
                "{}",
                msgr::respstring("Unknown Command", msgr::RespType::Error)
            );
        }
    }

    fn rl_get(&mut self) -> String {
        let mut readline = self.rl.readline(&format!(
            "({}) ",
            msgr::respstring(&self.name, msgr::RespType::Important)
        ));
        if readline.is_err() {
            return "".to_string();
        }

        readline = Ok(readline.unwrap());

        let line = readline.unwrap();
        let _ = self.rl.add_history_entry(&line);

        if line != "" {
            return line;
        }

        self.rl.history().iter().last().map_or("", |v| v).to_string()
    }

    pub fn new(name: &str) -> Self {
        let mut rl = DefaultEditor::new().unwrap();

        if rl.load_history("target/.rl_history").is_err() {
            println!(
                "{}",
                msgr::respstring("No previous history.", msgr::RespType::Warning)
            );
        }

        CommandManager {
            name: name.to_string(),
            rl,
        }
    }
}

impl Drop for CommandManager {
    fn drop(&mut self) {
        if self.rl.save_history("target/.rl_history").is_err() {
            println!(
                "{}",
                msgr::respstring("History not exist, Creating...", msgr::RespType::Warning)
            );
            std::fs::File::create("target/.rl_history").unwrap();
        }
    }
}
