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
  const char *usage;
  int (*handler) (char *);
} cmd_table [] = {
  { "help"  , "Display information about all supported commands"                    \
  
  , "\"help\" display all command and there discription \n\"help \'args\'\" shows single command's discription and it's usage", cmd_help },
  
  { "c"     , "Continue the execution of the program"                               \
  
  , "NONE", cmd_c },
  
  { "q"     , "Exit NEMU"                                                           \
  
  , "NONE", cmd_q },
  
  { "si"    , "Let the program step through N instructions and then pause execution"\
  
  , "\"si\" run 1 inst on nemu which same as \"si 1\" \n\"si \'N\'\" run N inst", cmd_si},
  
  { "info"  , "get some machine info"                                               \
  
  , "\"info r\" can show all register value and \"info r \'reg\'\" can show the specific register's value \n\"info w\" can show all wtachpoints 's message", cmd_info},
  
  { "x"     , "Scan Memory"                                                         \
  
  , "", cmd_x},

  { "ir", "printf instruction ring buffer"
  , "", cmd_ir}
};

#define NR_CMD ARRLEN(cmd_table)

#endif