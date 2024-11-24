#include <utils.hpp>

#include <readline/readline.h>
#include <readline/history.h>

ModuleState module_state = { .state = MODULE_STOP };

static char* rl_gets() {
    static char *line_read = NULL;

    if (line_read) {
        free(line_read);
        line_read = NULL;
    }

    line_read = readline("(npc) ");

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

void module_exec(uint64_t n);
int cmd_c(char *args) {
    module_exec(-1);
    return 0;
}

int cmd_q(char *args) {
    module_state.state = MODULE_STOP;
    return -1;
}

int cmd_sc(char *args) {
    char* parameter_str = strtok(args, " ");

    if(parameter_str == NULL){
        module_exec(1);
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

    module_exec(parameter);
    return 0;
}

void reset(int n);
int cmd_r(char *args) {
    reset(10);
    return 0;
}

void wave_Trace_on();
int cmd_wo(char *args) {
    wave_Trace_on();
    return 0;
}

static struct {
  const char *name;
  const char *description;
  const char *usage;
  int (*handler) (char *);
} cmd_table [] {
    {"c", "Continue the execution of the program", "NONE", cmd_c},
    {"q", "Exit NEMU",                             "NONE", cmd_q},
    {"wo","Begin Wave Trace",                      "NONE", cmd_wo},
    {"sc","Let the program step through N clk",    "NONE", cmd_sc},
    {"r", "Reset simulation",                      "NONE", cmd_r},
};

#define NR_CMD ARRLEN(cmd_table)

void sdb_mainloop(void) {
    extern bool is_batch_mode;
    if (is_batch_mode) {
        cmd_c(NULL); 
        return;
    }

    for(char *str; (str = rl_gets()) != NULL; ) {
        char *str_end = str + strlen(str);
        
        char *cmd = strtok(str, " ");
        if (cmd == NULL)  continue; 
        
        char *args = cmd + strlen(cmd) + 1;
        if (args >= str_end) args = NULL;
        
        int i;

        for (i = 0; i < NR_CMD; i++) {
            if (strcmp(cmd, cmd_table[i].name) != 0) continue;
            if (cmd_table[i].handler(args) < 0) { return;}
            break;
        }

        if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
    }
}
