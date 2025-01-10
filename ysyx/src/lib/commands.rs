use clap::{Parser, Subcommand};
use crate::msg_resp as msgr;

#[derive(Parser, Debug)]
#[command(author, version, about)]
pub struct Cli {
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
    pub fn new(name: &str) -> Self {
        let mut rl = DefaultEditor::new().unwrap();
        
        if rl.load_history("cache/.rl_history").is_err() {
            println!("{}", msgr::respstring("No previous history.", msgr::RespType::Warning));
        }

        CommandManager {
            name: name.to_string(),
            rl,
        }
    }

    fn rl_get(&mut self) -> String {
        let readline = self.rl.readline(&format!("({}) ", msgr::respstring(&self.name, msgr::RespType::Important)));
        match readline {
            Ok(line) => {
                let _ = self.rl.add_history_entry(&line);
                if line == "" {
                    self.rl.history().iter().last().map_or("", |v| v).to_string()
                } else {
                    line
                }
            },
            Err(_) => {
                "".to_string()
            },
        }
    }

    pub fn get_parser(&mut self) -> Commands {
        loop{
            let input = self.rl_get();

            let mut input: Vec<&str> = input.trim().split_whitespace().collect::<Vec<&str>>();
            if input.is_empty() { continue; }

            input.insert(0, ""); // Insert a dummy value

            let cli = Cli::try_parse_from(input);

            if let Err(cli) = cli {
                if cli.kind() == clap::error::ErrorKind::DisplayHelp {
                    println!("{}", cli.default_color());
                    continue;
                }
                println!("{}", msgr::respstring("Unknown Command", msgr::RespType::Error));
                continue;
            }

            return cli.unwrap().command;
        }
    }
}

impl Drop for CommandManager {
    fn drop(&mut self) {
        if self.rl.save_history("cache/.rl_history").is_err() {
            println!("{}", msgr::respstring("Failed to save history.", msgr::RespType::Error));
        }
    }
}