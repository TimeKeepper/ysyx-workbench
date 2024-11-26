#include <utils.hpp>
#include <simple_debugger.hpp>
#include <readline/readline.h>
#include <readline/history.h>

ModuleState module_state = { .state = MODULE_STOP };

static int cmd_c(Emulator& emulator, char *args) {
    emulator.cycle(-1);
    return 0;
}

static int cmd_q(Emulator& emulator, char *args) {
    module_state.state = MODULE_STOP;
    return -1;
}

static int cmd_sc(Emulator& emulator, char *args) {
    char* parameter_str = strtok(args, " ");

    if(parameter_str == NULL){
        emulator.cycle(1);
        return 0;
    }

    int parameter = atoi(parameter_str);
    if(parameter < 0){
        printf(ANSI_FMT("You should input a positive value\n", ANSI_FG_RED));
        return 0;
    }
    else if(parameter == 0){
        printf(ANSI_FMT("What do you mean, Bro?\n", ANSI_FG_RED));
        return 0;
    }

    emulator.cycle(parameter);
    return 0;
}

static int cmd_r(Emulator& emulator, char *args) {
    emulator.reset(20);
    return 0;
}

static int cmd_wo(Emulator& emulator, char *args) {
    emulator.wave_trace_ctrl(true);
    return 0;
}

void simple_debugger::set_batch_mode(){
    Log("Entering batch mode");
    is_batch_mode = true;
}

#include <getopt.h>
int simple_debugger::parse_args(int argc, char *argv[]) {
    const struct option table[] = {
      {"batch"    , no_argument      , NULL, 'b'},
      {0          , 0                , NULL,  0 },
    };
    int o;
    while ( (o = getopt_long(argc, argv, "-bhl:d:p:e:", table, NULL)) != -1) {
      switch (o) {
        case 'b': set_batch_mode();     break;
        default:
          printf("Usage: %s [OPTION...] IMAGE [args]\n\n", argv[0]);
          printf("\t-b,--batch              run with batch mode\n");
          printf("\n");
          exit(0);
      }
    }
    return 0;
}

#include <csignal>
void SIGINT_handler(int signal){
    if (signal == SIGINT) {
        printf(ANSI_FMT("\nsimulation interrupted\n", ANSI_FG_BLUE));
        module_state.state = MODULE_STOP;
    }
}

simple_debugger::simple_debugger(Emulator& emulator, int argc, char **argv) : emulator(emulator) {
    parse_args(argc, argv);
    
    signal(SIGINT, SIGINT_handler);

    cmd_table.push_back({"c", "Continue the execution of the program", "NONE", cmd_c});
    cmd_table.push_back({"q", "Exit NEMU",                             "NONE", cmd_q});
    cmd_table.push_back({"wo","Begin Wave Trace",                      "NONE", cmd_wo});
    cmd_table.push_back({"sc","Let the program step through N clk",    "NONE", cmd_sc});
}

simple_debugger::~simple_debugger() {
    cmd_table.clear();
}

static char* rl_gets() {
    static char *line_read = NULL;

    if (line_read) {
        free(line_read);
        line_read = NULL;
    }

    line_read = readline("(Zyqn) ");

    if(history_length != 0){
        HIST_ENTRY *last_cmd = history_get(history_length);
        
        if(strcmp(last_cmd->line, line_read) == 0){
            return line_read;
        }

        if(strcmp(line_read, "") == 0){
            line_read = (char*)malloc(strlen(last_cmd->line) + 1);
            strcpy(line_read, last_cmd->line);
            return line_read;
        }
    }

    if (line_read && *line_read) {
        add_history(line_read); 
    }

    return line_read;
}

void simple_debugger::sdb_mainloop(){
    if (is_batch_mode) {
        cmd_c(emulator, NULL); 
        return;
    }

    for(char *str; (str = rl_gets()) != NULL; ) {
        char *str_end = str + strlen(str);
        
        char *cmd = strtok(str, " ");
        if (cmd == NULL)  continue; 
        
        char *args = cmd + strlen(cmd) + 1;
        if (args >= str_end) args = NULL;
        
        int i;

        for (i = 0; i < cmd_table.size(); i++) {
            if (strcmp(cmd, cmd_table[i].name) != 0) continue;
            if (cmd_table[i].handler(emulator, args) < 0) { return;}
            break;
        }

        if (i == cmd_table.size()) { printf("Unknown command '%s'\n", cmd); }
    }
}
