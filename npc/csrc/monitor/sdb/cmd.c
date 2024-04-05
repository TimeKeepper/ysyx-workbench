#include <sdb/cmd.h>
#include <sdb/sdb.h>
#include <cpu/cpu.h>
#include <utils.h>
#include <memory/paddr.h>

int cmd_help(char *args) {
    /* extract the first argument */
    char *arg = strtok(NULL, " ");
    int i;

    if (arg == NULL) {
        /* no argument given */
        for (i = 0; i < NR_CMD; i ++) {
        printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
        }
    }
    else {
        for (i = 0; i < NR_CMD; i ++) {
            if (strcmp(arg, cmd_table[i].name) != 0) continue;
            printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
            return 0;
        }
        printf("Unknown command '%s'\n", arg);
    }

    return 0;
}

int cmd_c(char *args){
    cpu_exec(-1);
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

int cmd_si(char *args){
    char *arg = strtok(NULL, " ");
    int n = 1;
    if (arg != NULL) {
        sscanf(arg, "%d", &n);
    }
    cpu_exec(n);
    return 0;
}

int cmd_info(char *args){
    char* show_type = strtok(NULL, " ");
    if(show_type == NULL){
        printf("You should input the requried info type: r(register) or w(watchpoint).\n");
        return 0;
    }
    if(strlen(show_type) != 1) {
        printf("You should only enter an single character.\n");
        return 0;
    }
    char *specific_info = strtok(NULL, " ");
    switch(*show_type){
        case 'r': isa_reg_display(specific_info);break;
        case 'w': break;
        default: printf("you should input the requried info type: r(register) or w(watchpoint).\n");
    }
    return 0;
}

static uint32_t print_Ram(uint32_t bias){
    uint32_t result = paddr_read(bias, 4);
    printf("0x%08x ", result);
    return result;
}

int cmd_x(char *args){
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
    return 0;
}
