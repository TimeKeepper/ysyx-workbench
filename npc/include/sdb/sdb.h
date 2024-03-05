#ifndef __MONITOR_H__
#define __MONITOR_H__

#include <sdb/cmd.h>

void init_monitor(int argc, char *argv[]);
void sdb_mainloop();
void init_regex();
word_t expr(char *e, bool *success);
void init_sdb();
char* get_func_name(long addr);

static struct {
    const char *name;
    const char *description;
    int (*handler) (char *);
    } cmd_table [] = {
    { "help", "Display information about all supported commands", cmd_help },
    { "c", "Continue the execution of the program", cmd_c },
    { "q", "Exit NEMU", cmd_q },
    { "r", "Reset npc", cmd_r},
    { "si", "Let the program step through N instructions and then pause execution", cmd_si},
    { "info", "get some machine info", cmd_info},{ "x", "Scan Memory", cmd_x},

    /* TODO: Add more commands */

};

#define NR_CMD ARRLEN(cmd_table)

#endif