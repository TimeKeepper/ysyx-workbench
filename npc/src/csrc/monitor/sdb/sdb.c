#include <common.h>
#include <sdb/sdb.h>
#include <cpu/cpu.h>
#include <readline/readline.h>
#include <readline/history.h>
// #include <csignal>
// #include <cstdlib>

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


int is_batch_mode = false;

void sdb_set_batch_mode() {
    Log("Entering batch mode");
    is_batch_mode = true;
}

void wave_Trace_close(void);

void sdb_exit() {
    #ifdef CONFIG_WTRACE
    wave_Trace_close();
    #endif

    #ifdef CONFIG_NVBOARD
    nvboard_quit();
    #endif

    #if NAME==microbench
    Log("Generating report...");
    double ipc = (double)inst_cnt / (double)clk_cnt;

    const char* npc_path = getenv("NPC_HOME");
    char report_path[512];
    FILE* report_file;

    if(npc_path == NULL){
        Log("NPC_HOME not set");
        goto report_end;
    }

    snprintf(report_path, sizeof(report_path), "%s/platform/core/build/report.txt", npc_path);
    report_file = fopen(report_path, "w");
    
    if(report_file == NULL){
        Log("Failed to open report file");
        goto report_end;
    }

    fprintf(report_file, "%lu\n", inst_cnt);
    fprintf(report_file, "%lu\n", clk_cnt);
    fprintf(report_file, "%lf\n", ipc);
    fprintf(report_file, "%lu\n", IFU_pc);
    fprintf(report_file, "%lu\n", LSU_pc);
    fprintf(report_file, "%lu\n", ALU_pc);
    fprintf(report_file, "%lu\n", i_LS);
    fprintf(report_file, "%lu\n", i_CSR);
    fprintf(report_file, "%lu\n", i_Cal);
    fprintf(report_file, "%lu\n", c_LS);
    fprintf(report_file, "%lu\n", c_CSR);
    fprintf(report_file, "%lu\n", c_Cal);

    fclose(report_file);

    report_end: 
    Log("Generated end");

    #endif
}

void sdb_mainloop() {
    if (is_batch_mode) {
        cmd_c(NULL);
        return;
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
            if (cmd_table[i].handler(args) < 0) { sdb_exit(); return;}
            break;
        }

        if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
    }
}

void init_wp_pool();

void init_sdb(){
    init_regex();

    init_wp_pool();
}
