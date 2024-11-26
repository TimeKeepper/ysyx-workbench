#include "memory/paddr.hpp"
#include "utils.hpp"
#include "cpu/cpu.hpp"
#include <sdb/sdb.hpp>

uint64_t IFU_pc = 0, LSU_pc = 0, ALU_pc = 0;

uint64_t i_CSR = 0, i_LS = 0, i_Cal = 0;
uint64_t c_CSR = 0, c_LS = 0, c_Cal = 0;

uint64_t inst_cnt = 0;

uint64_t num_of_inst_to_end = 0;

uint64_t icache_hit = 0, icache_map_hit = 0;

extern "C" void IFU_finished(uint32_t cache_hit, uint32_t map_hit) {
    if(map_hit) {
        icache_map_hit++;
        if(cache_hit) {
            icache_hit++;
        }
    }
    IFU_pc++;
}

extern "C" void LSU_finished() {
    LSU_pc++;
}

extern "C" void ALU_finished() {
    ALU_pc++;
}

static uint32_t  l_iType = 0;

extern "C" void IDU_finished(uint32_t iType) {
    switch (iType) {
        case 0: i_LS ++; break;
        case 1: i_CSR ++; break;
        case 2: i_Cal ++; break;
    }
    l_iType = iType;
}

static void ID_clk(){
    static uint64_t l_clk = 0;
    switch (l_iType) {
        case 0: c_LS += clk_cnt - l_clk; break;
        case 1: c_CSR += clk_cnt - l_clk; break;
        case 2: c_Cal += clk_cnt - l_clk; break;
    }
    l_clk = clk_cnt;
}

static void func_called_detect(){
    #ifdef CONFIG_FTRACE
    static uint32_t stack_num = 0;

    static char* last_func_name = NULL;
    struct get_func msg = get_func_name(cpu.pc);
    char* func_name = msg.name;
    if(func_name != NULL && last_func_name != func_name){
        if(!msg.is_call) {printf("ret  "); stack_num--;}
        else {printf("call "); stack_num++;}

        for(int i = 0; i < stack_num; i++) printf(" ");
        printf("[%s]\n", func_name);

        last_func_name = func_name;
    }
    #endif
}

void watchpoint_catch(void){
    #ifdef CONFIG_WATCHPOINT
    wp_Value_Update();
    WP* wp;
    for(int i = 0; (wp = get_Changed_wp(i)) != NULL; i++){
        printf("Watchpoint %d: " ANSI_FMT("%s\n", ANSI_FG_BLUE), wp->NO, wp->expr);
        printf(ANSI_FMT("Old value" , ANSI_FG_YELLOW)  " = 0x%08x\n", wp->last_time_Value);
        printf(ANSI_FMT("New value" , ANSI_FG_GREEN) " = 0x%08x\n", wp->value);
        num_of_inst_to_end = 0;
        // if(npc_state.state != NPC_END) npc_state.state = NPC_STOP;//如果在npc停止的情况下修改state，就会导致报错,因为会导致检查trap的时候无法通过NPC_END的判断
    }
    #endif
}

static uint32_t npc = RESET_VECTOR;

extern "C" void pc_update(uint32_t n_npc){
    npc = n_npc;
}

static bool is_comp_first_time = false;

extern "C" void inst_comp_update(){
    if(!is_comp_first_time){
        is_comp_first_time = true;
        return;
    }
    inst_cnt++;
    num_of_inst_to_end = num_of_inst_to_end == 0 ? 0 : num_of_inst_to_end - 1;
    difftest_step(cpu.pc, npc);
    
    watchpoint_catch();          //检查watchpoint

    func_called_detect();   

    ID_clk();
}
