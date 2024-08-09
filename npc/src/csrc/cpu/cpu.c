#include "VysyxSoCFull__Dpi.h"
#define STRINGIFY(x) #x
#define TOSTRING(x) STRINGIFY(x)

#ifdef DEFINE_NPC
#include "Vtop.h"
#include "Vtop___024root.h"
#define DUT_PC top->rootp->top__DOT__npc__DOT__cpu__DOT__REG__DOT__pc
#else
#include "VysyxSoCFull.h"
#include "VysyxSoCFull___024root.h"
#define DUT_PC top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__REG__DOT__pc
#endif
#include "verilated_vcd_c.h"
#include <cpu/cpu.h>
#include <memory/paddr.h>
#include <utils.h>
#include <sdb/sdb.h>

VerilatedContext* contextp = new VerilatedContext;
TOP_NAME* top = new TOP_NAME{contextp};

uint32_t clk_cnt = 0;
uint32_t inst_cnt = 0;
bool     is_itrace_printf = false;

#define MAX_INST_TO_PRINT 10
#define INSTR_BUF_SIZE 15
#define INST_SIZE 128
char INST_BUF[INSTR_BUF_SIZE][INST_SIZE];
static int instr_buf_index = 0;

void instr_buf_push(char *instr){
  if(++instr_buf_index >= INSTR_BUF_SIZE){
    instr_buf_index = 0;
  }
  strcpy(INST_BUF[instr_buf_index], instr);
}

void instr_buf_printf(void){
  for(int i = 0; i < INSTR_BUF_SIZE; i++){
    i == instr_buf_index ? printf("---> ") : printf("     ");
    printf("%s\n", INST_BUF[i]);
  }
}

const char *regs[32] = {
  "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
  "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
  "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
  "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
};
VerilatedVcdC* tfp = new VerilatedVcdC;

void wave_Trace_init(int argc, char **argv){
    contextp->commandArgs(argc, argv);
    #ifdef WAVE_TRACE
    Verilated::traceEverOn(true);
    top->trace(tfp, 99);
    tfp->open("wave.vcd");
    #endif
}

void wave_Trace_once(){
    #ifdef WAVE_TRACE
    contextp->timeInc(1);
    tfp->dump(contextp->time());
    #endif
}

void wave_Trace_close(){
    #ifdef WAVE_TRACE
    tfp->close();
    #endif
}

CPU_State cpu = {.gpr = {0}, .pc = 0x80000000, .sr = {0}};

void difftest_skip_ref();

void my_putc(int c){
    difftest_skip_ref();
    putc((char)c, stderr);
}

uint32_t ram_read(uint32_t addr, int len){
    return paddr_read(addr, len);
}

void ram_write(paddr_t addr, int len, word_t data){
    paddr_write(addr, len, data);
}

static void single_cycle() {
    top->clock = 0; top->eval();wave_Trace_once();                  

    top->clock = 1; top->eval();wave_Trace_once();        
    clk_cnt++;
}

static void reset(int n) {
    top->reset = 1;
    while (n -- > 0) single_cycle();
    top->reset = 0;
}

void cpu_reset(int n, int argc, char **argv){
    if(argv != NULL) wave_Trace_init(argc, argv);
    
    reset(n); 
    clk_cnt = 0;
}

void cpu_value_update(uint8_t pc_wen, uint8_t csra_wen, uint8_t csrb_wen, uint8_t gpr_wen, uint32_t new_PC, uint32_t CSR_waddra, uint32_t new_CSRa, uint32_t CSR_waddrb, uint32_t new_CSRb, uint32_t GPR_waddr, uint32_t new_GPR){
    if(pc_wen) cpu.pc = new_PC;   
    if(csra_wen) cpu.sr[CSR_waddra] = new_CSRa; 
    if(csrb_wen) cpu.sr[CSR_waddrb] = new_CSRb; 

    if(gpr_wen) cpu.gpr[GPR_waddr] = new_GPR;
}

char itrace_buf[256];
void itrace_catch(uint32_t addr, uint32_t inst){
    #ifdef ITRACE

    char* p = itrace_buf;

    uint8_t* inst_ptr = (uint8_t*)&inst;
    p += snprintf(p, sizeof(itrace_buf),  "0x%08x: ", addr);

    for(int i = 3; i >= 0; i--){
        p += snprintf(p, 4, "%02x ", inst_ptr[i]);
    }
    disassemble(p, itrace_buf + sizeof(itrace_buf) - p, cpu.pc, inst_ptr, 4);

    instr_buf_push(itrace_buf);

    if(is_itrace_printf) printf("%s\n", itrace_buf);
    #endif
}

static bool is_ret = false;

static void func_called_detect(){
    #ifdef FTRACE
    static uint32_t stack_num = 0;

    static char* last_func_name = NULL;
    char* func_name = get_func_name(cpu.pc);
    if(func_name != NULL && last_func_name != func_name){
        if(is_ret) {printf("ret  "); is_ret = false; stack_num--;}
        else {printf("call "); stack_num++;}

        for(int i = 0; i < stack_num; i++) printf(" ");
        printf("[%s]\n", func_name);
    }
    last_func_name = func_name;
    #endif
}

static uint32_t num_of_inst_to_end = 0;

void watchpoint_catch(void){
    #ifdef CONFIG_WATCHPOINT
    wp_Value_Update();
    WP* wp;
    for(int i = 0; (wp = get_Changed_wp(i)) != NULL; i++){
        printf("Watchpoint %d: " ANSI_FMT("%s\n", ANSI_FG_BLUE), wp->NO, wp->expr);
        printf(ANSI_FMT("Old value" , ANSI_FG_YELLOW)  " = 0x%08x\n", wp->last_time_Value);
        printf(ANSI_FMT("New value" , ANSI_FG_GREEN) " = 0x%08x\n", wp->value);
        num_of_inst_to_end = 1;
        // if(npc_state.state != NPC_END) npc_state.state = NPC_STOP;//如果在npc停止的情况下修改state，就会导致报错,因为会导致检查trap的时候无法通过NPC_END的判断
    }
    #endif
}

void check_special_inst(uint32_t inst){
    switch(inst){
        case 0x00000000: npc_trap(1);   break; // ecall
        case 0xffffffff: npc_trap(1);   break; // bad trap
        case 0x00100073: npc_trap(cpu.gpr[10]);   break; // ebreak
        case 0x00008067: is_ret = true;     break; // ret
        default: break;
    }
}

void difftest_step(vaddr_t pc, vaddr_t npc);

static bool is_comp_first_time = true; // 由于多周期处理器特性不得不引入的边界条件，或许能够在修改成流水线之后去除

void inst_comp_update(){
    if(is_comp_first_time){
        is_comp_first_time = false;
        return;
    }
    inst_cnt++;
    num_of_inst_to_end = num_of_inst_to_end == 0 ? 0 : num_of_inst_to_end - 1;
    // difftest_step(cpu.pc, DUT_PC);
}

static void execute_one_clk(){
    // // nvboard_update();

    single_cycle();           
    
    watchpoint_catch();          //检查watchpoint

    func_called_detect();   
}

static void execute(uint64_t n){
    is_itrace_printf = (n < MAX_INST_TO_PRINT);
    num_of_inst_to_end = n;
    while(1){
        
        execute_one_clk();

        if(num_of_inst_to_end == 0) break;

        if(npc_state.state != NPC_RUNNING) break;
    }
}

void error_waddr(){
    printf(ANSI_FMT("Invalid write address\n", ANSI_FG_RED));
    npc_trap(-1);
}

int npc_trap (int a0){
    npc_state.state = NPC_END;
    npc_state.halt_ret = a0;
    printf(ANSI_FMT("a0: %d inst: %d\n", ANSI_FG_BLUE), a0, inst_cnt);
    if(a0 == 0) printf("\033[1;32mHit good trap\033[0m\n");
    else printf("\033[1;31mHit bad trap\033[0m\n");
    wave_Trace_once();
    wave_Trace_close(); 
    return clk_cnt;
}

void clk_exec(uint64_t n){
    for(;n > 0; n--) execute_one_clk();
}

void cpu_exec(uint64_t n){
    switch (npc_state.state) {
        case NPC_END: case NPC_ABORT:
            printf("Simulation has completed, enter r to restart program\n");
            return;
        default: npc_state.state = NPC_RUNNING;
    }

    execute(n);
}

char* reg_id2name(int id){
    if(id < 0 || id > 31) return NULL;
    return (char*)regs[id];
}

int reg_name2id(char *reg_name){
    for(int i = 0; i < 32; i++){
        if(strcmp(regs[i], reg_name) == 0) return i;
    }
    return -1;
}

void isa_reg_display(char *reg_name){
    if(reg_name == NULL){
        for(int i = 0; i < 32; i++){
            printf(ANSI_FMT("%s\t", ANSI_FG_BLUE) "0x%08x\n", regs[i], cpu.gpr[i]);
        }
        return;
    }

    if(strcmp(reg_name, "pc") == 0){
        printf(ANSI_FMT("pc\t", ANSI_FG_BLUE) "0x%08x\n", cpu.pc);
        return;
    }

    int reg_num = reg_name2id(reg_name);
    if(reg_num < 0 || reg_num > 31){
        printf(ANSI_FMT("Invalid register number\n", ANSI_FG_RED));
        return;
    }

    printf("%s: 0x%08x\n", reg_name, cpu.gpr[reg_num]);
}
