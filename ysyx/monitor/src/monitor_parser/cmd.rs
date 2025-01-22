use msg_resp as msgr;
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
    /// quit the program, not need any arguments
    #[clap(visible_alias = "q")]
    Quit {},

    /// run single instrcution in the emulator
    #[clap(visible_alias = "si")]
    SingleInstrcution {
        count: Option<u32>,
    },

    /// continue the emulator
    #[clap(visible_alias = "c")]
    Continue {},

    /// show info about the simulator
    #[clap(visible_alias = "i")]
    Info {
        #[command(subcommand)]
        command: InfoCommands,
    },

    /// show instruction ringbuffer
    #[clap(visible_alias = "ir")]
    InstructionRingBuffer {},

    /// Times printf
    #[clap(visible_alias = "t")]
    Times {},

    /// control function of the simulator
    #[clap(visible_alias = "f")]
    Function {
        on_or_off: Option<String>,
        target: Option<String>,
    },
}

#[derive(Debug, Subcommand)]
pub enum InfoCommands {
    /// show info about the register
    #[clap(visible_alias = "r")]
    Register {
        target: Option<String>,
    },
}

use owo_colors::OwoColorize;

use rustyline::completion::FilenameCompleter;
use rustyline::highlight::MatchingBracketHighlighter;
use rustyline::hint::HistoryHinter;
use rustyline::validate::MatchingBracketValidator;
use rustyline::{Cmd, CompletionType, Config, EditMode, Editor, KeyEvent};
pub struct CommandManager {
    name: String,
    rl: Editor<super::MyHelper, rustyline::history::FileHistory>,
}

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
        let p = &format!(
            "({}) ",
            msgr::respstring(&self.name, msgr::RespType::Important)
        );
        self.rl.helper_mut().expect("No helper").colored_prompt = format!("{p}");

        let readline = self.rl.readline(&p);
        if readline.is_err() {
            return "".to_string();
        }

        let line = readline.unwrap();
        let _ = self.rl.add_history_entry(&line);

        if line != "" {
            return line;
        }

        self.rl.history().iter().last().map_or("", |v| v).to_string()
    }

    pub fn new(name: &str) -> Self {
        let config = Config::builder()
            .history_ignore_space(true)
            .completion_type(CompletionType::List)
            .edit_mode(EditMode::Emacs)
            .build();
        let h = super::MyHelper {
            completer: FilenameCompleter::new(),
            highlighter: MatchingBracketHighlighter::new(),
            hinter: HistoryHinter::new(),
            colored_prompt: "".to_owned(),
            validator: MatchingBracketValidator::new(),
        };
        let mut rl = Editor::with_config(config).unwrap();

        rl.set_helper(Some(h));
        rl.bind_sequence(KeyEvent::alt('n'), Cmd::HistorySearchForward);
        rl.bind_sequence(KeyEvent::alt('p'), Cmd::HistorySearchBackward);
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
