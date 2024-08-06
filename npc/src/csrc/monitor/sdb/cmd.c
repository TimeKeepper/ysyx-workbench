#include <sdb/cmd.h>
#include <sdb/sdb.h>
#include <cpu/cpu.h>
#include <stdexcept>
#include <utils.h>
#include <memory/paddr.h>

int cmd_help(char *args) {
    /* extract the first argument */
    char *arg = strtok(NULL, " ");
    int i;

    if (arg == NULL) {
        /* no argument given */
        for (i = 0; i < NR_CMD; i ++) {
            printf(ANSI_FMT("%s\t", ANSI_FG_BLUE) " - " ANSI_FMT("%s\n", ANSI_FG_MAGENTA), cmd_table[i].name, cmd_table[i].description);
        }
        return 0;
    }
    
    for (i = 0; i < NR_CMD; i ++) {
        if (strcmp(arg, cmd_table[i].name) == 0) {
            printf(ANSI_FMT("%s\t", ANSI_FG_BLUE) " - " ANSI_FMT("%s\n", ANSI_FG_MAGENTA) "usage: \n" ANSI_FMT("%s\n", ANSI_FG_CYAN), cmd_table[i].name, cmd_table[i].description, cmd_table[i].usage);
            return 0;
        }
    }

    printf(ANSI_FMT("Unknown command", ANSI_FG_RED) " '%s'\n", arg);
    return 0;
}

int cmd_c(char *args){
    cpu_exec(-1);
    return 0;
}

extern uint32_t clk_cnt;
extern uint32_t inst_cnt;
int cmd_t(char *args){
    printf(ANSI_FMT("Current Clk times:", ANSI_FG_BLUE) ANSI_FMT(" %d\n", ANSI_FG_MAGENTA), clk_cnt);
    printf(ANSI_FMT("Current inst nums:", ANSI_FG_BLUE) ANSI_FMT(" %d\n", ANSI_FG_MAGENTA), inst_cnt);
    return 0;
}

int cmd_q(char *args){
    npc_state.state = NPC_QUIT;
    return -1;
}

int cmd_r(char *args){
    cpu_reset(10, 0, NULL);
    return 0;
}

int cmd_si(char *args) {
    char* parameter_str = strtok(args, " ");

    if(parameter_str == NULL){
        cpu_exec(1);
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

    cpu_exec(parameter);
    return 0;
}

int cmd_sc(char *args) {
    char* parameter_str = strtok(args, " ");

    if(parameter_str == NULL){
        clk_exec(1);
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

    clk_exec(parameter);
    return 0;
}

int cmd_info(char *args) {
    char* show_type = strtok(args, " ");
    if(show_type == NULL){
        printf(ANSI_FMT("You should input the requried info type: r(register) or w(watchpoint).\n", ANSI_FG_RED));
        return 0;
    }
    if(strlen(show_type) != 1) {
        printf(ANSI_FMT("You should only enter an single character.\n", ANSI_FG_RED));
        return 0;
    }
    char *specific_info = strtok(NULL, " ");
    switch(*show_type){
        case 'r': isa_reg_display(specific_info);                                       break;
        case 'w': wp_display();                                                                 break;
        default : printf(ANSI_FMT("you should input the requried info type: r(register) or w(watchpoint).\n", ANSI_FG_RED));  break;
    }
    return 0;
}

static uint32_t print_Ram(uint32_t bias){
    uint32_t result = paddr_read(bias, 4);
    printf("0x%08x ", result);
    return result;
}

int cmd_x(char *args){
    if(args == NULL) {
        printf(ANSI_FMT("You should input the scan time!\n", ANSI_FG_RED));
        return 0;
    }
    
    int scan_num = atoi(strtok(args, " "));

    if(scan_num <=0 || scan_num > 100){
        printf(ANSI_FMT("The scan number should be in the range of 1 to 100!\n", ANSI_FG_RED));
        return 0;
    }
    
    bool success = true;
    uint32_t base_Addr = expr(strtok(NULL, " "), &success);

    if(!likely(in_pmem(base_Addr))){
        printf(ANSI_FMT("The 0x%08x address is out of range!\n", ANSI_FG_RED), base_Addr);
        return 0;
    }

    for(int i = 0; i < scan_num; i++){
        print_Ram(base_Addr + 4 * i);
        for(int j = 0; j < 4; j++){
        printf(ANSI_FMT("%c", ANSI_FG_BLUE), paddr_read(base_Addr + 4 * i + j, 1));
        }
        printf("\n");
    }
    return 0;
}

void instr_buf_printf(void);
int cmd_ir(char *args){
    #ifndef ITRACE
    printf(ANSI_FMT("You have no enable funtion named ITRACE\n", ANSI_FG_RED));
    return 0;
    #endif
    instr_buf_printf();
    return 0;
}

int cmd_w(char *args){
  #ifndef CONFIG_WATCHPOINT
    printf(ANSI_FMT("The watchpoint function is not enabled!\n", ANSI_FG_RED));
    return 0;
  #endif
  if(args == NULL){
    printf(ANSI_FMT("No expression!\n", ANSI_FG_RED));
    return 0;
  }
  new_wp(args);
  wp_Value_Update();
  return 0;
}

int cmd_d(char *args){
  if(args == NULL){
    printf(ANSI_FMT("No delete op!\n", ANSI_FG_RED));
    return 0;
  }
  int wpNO = atoi(args);
  WP* wp = get_head_wp();
  for(int i = 0; i < wpNO - 1; i++){
    wp = wp->next;
  }
  if(wp!=NULL) free_wp(wp);
  return 0;
}

int cmd_b(char *args){
  #ifndef CONFIG_WATCHPOINT
    printf(ANSI_FMT("The watchpoint function is not enabled!\n", ANSI_FG_RED));
    return 0;
  #endif
  if(args == NULL){
    printf(ANSI_FMT("You should input the address of the breakpoint!\n", ANSI_FG_RED));
    return 0;
  }
  bool success = true;
  word_t addr = expr(args, &success);
  if(!in_pmem(addr)){
    printf(ANSI_FMT("The 0x%08x address is out of range!\n", ANSI_FG_RED), addr);
    return 0;
  }
  char expr_str[20] = "$pc == ";
  strcat(expr_str, args);
  new_wp(expr_str);
  wp_Value_Update();
  return 0;
}


