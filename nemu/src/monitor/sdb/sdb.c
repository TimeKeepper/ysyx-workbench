/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <isa.h>
#include <cpu/cpu.h>
#include <readline/readline.h>
#include <readline/history.h>
#include <memory/paddr.h>
#include "sdb.h"
#include "common.h"
#include "debug.h"
#include "utils.h"

static int is_batch_mode = false;

void init_regex();
void init_wp_pool();

/* We use the `readline' library to provide more flexibility to read from stdin. */
static char* rl_gets() {
  static char *line_read = NULL;

  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(nemu) ");

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

static int cmd_help(char *args);
static int cmd_c(char *args);
static int cmd_q(char *args);
static int cmd_si(char *args);
static int cmd_info(char *args);
static int cmd_x(char *args);
static int cmd_w(char *args);
static int cmd_d(char *args);
static int cmd_b(char *args);
static int cmd_test(char *args);
static int cmd_single_test(char *args);
static int cmd_crv(char *args);

static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
  { "help", "Display information about all supported commands", cmd_help },
  { "c", "Continue the execution of the program", cmd_c },
  { "q", "Exit NEMU", cmd_q },
  { "si", "Let the program step through N instructions and then pause execution", cmd_si},
  { "info", "get some machine info", cmd_info},
  { "x", "Scan Memory", cmd_x},
  {"w", "create watchpoint", cmd_w},
  {"d", "delete watchpoint", cmd_d},
  {"b", "create breakpoint", cmd_b},
  {"test", "Help me for test my code", cmd_test},
  {"stest", "Help me for test my code", cmd_single_test},
  { "crv", "Changing risgister's value", cmd_crv}

  /* TODO: Add more commands */

};

#define NR_CMD ARRLEN(cmd_table)

static int cmd_help(char *args) {
  /* extract the first argument */
  char *arg = strtok(NULL, " ");
  int i;

  if (arg == NULL) {
    /* no argument given */
    for (i = 0; i < NR_CMD; i ++) {
      printf(ANSI_FMT("%s\t", ANSI_FG_BLUE) " - " ANSI_FMT("%s\n", ANSI_FG_MAGENTA), cmd_table[i].name, cmd_table[i].description);
    }
  }
  else {
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(arg, cmd_table[i].name) == 0) {
        printf("%s\t - %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}

static int cmd_c(char *args) {
  //input -1 as parameter to cpu_exec means continious execute command forever.
  cpu_exec(-1);
  return 0;
}

static int cmd_q(char *args) {
  nemu_state.state = NEMU_QUIT;
  return -1;
}

static int cmd_si(char *args) {
  char* parameter_str = strtok(args, " ");
  if(parameter_str == NULL){
    cpu_exec(1);
  }
  else{
    int parameter = atoi(parameter_str);
    cpu_exec(parameter);
  }
  return 0;
}

static int cmd_info(char *args) {
  char* show_type = strtok(args, " ");
  if(show_type == NULL){
    Log("You should input the requried info type: r(register) or w(watchpoint).");
    return 0;
  }
  if(strlen(show_type) != 1) {
    Log("You should only enter an single character.");
    return 0;
  }
  char *specific_info = strtok(NULL, " ");
  switch(*show_type){
    case 'r': isa_reg_display(specific_info);break;
    case 'w': wp_display();break;
    default:Log("you should input the requried info type: r(register) or w(watchpoint).");break;
  }
  return 0;
}

static uint32_t print_Ram(uint32_t bias){
  uint32_t result = paddr_read(bias, 4);
  printf("0x%08x ", result);
  return result;
}

static int cmd_x(char *args){
  char *scan_num_str = strtok(args, " ");
  int scan_num = atoi(scan_num_str);
  bool success = true;
  uint32_t base_Addr = expr(scan_num_str+strlen(scan_num_str)+1, &success);
  if(!likely(in_pmem(base_Addr))){
    printf("The 0x%08x address is out of range!\n", base_Addr);
    return 0;
  }
  for(int i = 0; i < scan_num; i++){
    print_Ram(base_Addr + 4 * i);
    for(int j = 0; j < 4; j++){
      printf("%c", paddr_read(base_Addr + 4 * i + j, 1));
    }
    printf("\n");
  }
  printf("\n");
  return 0;
}

static int cmd_w(char *args){
  new_wp(args);
  wp_Value_Update();
  return 0;
}

static int cmd_d(char *args){
  int wpNO = atoi(args);
  WP* wp = get_head_wp();
  for(int i = 0; i < wpNO - 1; i++){
    wp = wp->next;
  }
  if(wp!=NULL) free_wp(wp);
  return 0;
}

static int cmd_b(char *args){
  if(args == NULL){
    printf("You should input the address of the breakpoint!\n");
    return 0;
  }
  bool success = true;
  word_t addr = expr(args, &success);
  if(addr < 0x80000000){
    printf("The address is out of range!\n");
    return 0;
  }
  char expr_str[20] = "$pc == ";
  strcat(expr_str, args);
  new_wp(expr_str);
  wp_Value_Update();
  return 0;
}

// #define INPUT_BUF_LENGTH 65536
// char input_buf[INPUT_BUF_LENGTH];

static int cmd_test(char *args){
  TODO();
  // bool success = true;

  // FILE* fp = fopen("/home/wen-jiu/my_ysyx_project/ysyx-workbench/nemu/tools/gen-expr/input", "r");

  // if(fp == NULL){
  //   printf("Can not open the file!\n");
  //   return 0;
  // }

  // while(fgets(input_buf, INPUT_BUF_LENGTH, fp) != NULL){
  //   char* result_str = strtok(input_buf, " ");
  //   char* expr_str = result_str + strlen(result_str) + 1;
  //   expr_str[strlen(expr_str) - 1] = '\0';
  //   int result = atoi(result_str);
  //   printf("expr: %s, result: %d\n", expr_str, result);
  //   if(expr(expr_str, &success) != result){
  //     printf("Test failed! The result should be %d, but your result is %d\n", result, expr(expr_str, &success));
  //   }
  // }

  // return 0;
}

static int cmd_single_test(char *args){
  TODO();
  // bool success = true;
  // char* expr_str = args;
  // int result = atoi(args + strlen(args) + 1);
  // printf("expr: %s, result: %d\n", expr_str, result);
  // if(expr(expr_str, &success) != result){
  //   printf("Test failed! The result should be %d, but your result is %d\n", result, expr(expr_str, &success));
  // }
  // return 0;
}

void change_register_value(int, word_t);

static int cmd_crv(char *args){
  char* reg_name = strtok(args, " ");
  if(reg_name == NULL){
    Log("You should input the register name!");
    return 0;
  }
  char* reg_value_str = strtok(NULL, " ");
  if(reg_value_str == NULL){
    Log("You should input the register value!");
    return 0;
  }
  bool success = true;
  word_t reg_value = expr(reg_value_str, &success);
  if(success){
    int regNO = isa_str2id(reg_name, &success);
    change_register_value(regNO, reg_value);
  }
  return 0;
}

void sdb_set_batch_mode() {
  is_batch_mode = true;
}

void sdb_mainloop() {
  if (is_batch_mode) {
    cmd_c(NULL);
    return;
  }

  for (char *str; (str = rl_gets()) != NULL; ) {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL) { continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = NULL;
    }

#ifdef CONFIG_DEVICE
    extern void sdl_clear_event_queue();
    sdl_clear_event_queue();
#endif

    int i;
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) { return; }
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
  }
}

void init_sdb() {
  /* Compile the regular expressions. */
  init_regex();

  /* Initialize the watchpoint pool. */
  init_wp_pool();
}
