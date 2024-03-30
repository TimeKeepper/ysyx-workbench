#include <common.h>
#include <sdb/sdb.h>
#include <cpu/cpu.h>
#include <readline/readline.h>
#include <readline/history.h>

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


static int is_batch_mode = false;

void sdb_set_batch_mode() {
  is_batch_mode = true;
}

void sdb_mainloop() {
    if (is_batch_mode) {
        cmd_c(NULL);
        // return;
    }

    for(char *str; (str = rl_gets()) != NULL; ) {
        char *str_end = str + strlen(str);

        /* extract the first token as the command */
        char *cmd = strtok(str, " ");
        if (cmd == NULL)  continue; 

        /* treat the remaining string as the arguments,
        * which may need further parsing
        */
        char *args = cmd + strlen(cmd) + 1;
        if (args >= str_end) args = NULL;

        int i;

        for (i = 0; i < NR_CMD; i++) {
            if (strcmp(cmd, cmd_table[i].name) != 0) continue;
            if (cmd_table[i].handler(args) < 0) { return; }
            break;
        }

        if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
    }

    // while(1) {
    //     if(!cpu_exec(1)) break;
    // }
}

void init_sdb(){
    init_regex();
}
