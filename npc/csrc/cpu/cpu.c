#include "Vtop___024root.h"
#include "verilated_vcd_c.h"
#include <cpu/cpu.h>
#include <cstdint>
#include <memory/paddr.h>
#include <utils.h>
#include <sdb/sdb.h>

TOP_NAME dut;
uint32_t clk_cnt = 0;

const char *regs[32] = {
  "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
  "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
  "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
  "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
};

struct Helper {
    Helper() {
        Verilated::traceEverOn(true);
    }
};
Helper helper;
const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
VerilatedVcdC* tfp = new VerilatedVcdC;

void wave_Trace_init(int argc, char **argv){
    #ifdef TRACE
    contextp->commandArgs(argc, argv);
    dut.trace(tfp, 99);
    tfp->open("wave.vcd");
    #endif
}

void wave_Trace_once(){
    #ifdef TRACE
    contextp->timeInc(1);
    tfp->dump(contextp->time());
    #endif
}

void wave_Trace_close(){
    #ifdef TRACE
    tfp->close();
    #endif
}

CPU_State cpu = {.gpr = {0}, .pc = 0x80000000, .sr = {0}};

uint32_t ram_read(uint32_t addr, int len){
    return paddr_read(addr, len);
}

void ram_write(paddr_t addr, int len, word_t data){
    paddr_write(addr, len, data);
}

uint32_t memory_read(void){
    uint32_t mem_addr = dut.rootp->mem_addr;
    if (!(likely(in_pmem(mem_addr)) || (mem_addr == RTC_ADDR) || (mem_addr == RTC_ADDR + 4))) return 0;
    switch(dut.rootp->memop){
        case 0b010: /*printf("Hit as 0b010!!!\n");*/return ram_read(mem_addr,  4);
        case 0b101: /*printf("Hit as 0b101!!!\n");*/return ram_read(mem_addr,  2);
        case 0b100: /*printf("Hit as 0b100!!!\n");*/return ram_read(mem_addr,  1);
        case 0b001: /*printf("Hit as 0b001!!!\n");*/return SEXT(ram_read(mem_addr,  2), 16);
        case 0b000: /*printf("Hit as 0b000!!!\n");*/return SEXT(ram_read(mem_addr,  1), 8);
        default: return 0;
    }
}

void memory_write(void){
    uint32_t mem_addr = dut.rootp->mem_addr;
    if (!((likely(in_pmem(mem_addr))) || mem_addr == SERIAL_PORT)) return;
    switch(dut.rootp->memop){
        case 0b010: ram_write(mem_addr,  4, dut.rootp->memdata); break;
        case 0b001:
        case 0b101: ram_write(mem_addr,  2, dut.rootp->memdata); break;
        case 0b000:
        case 0b100: ram_write(mem_addr,  1, dut.rootp->memdata); break;
        default: break;
    }
}

static void single_cycle() {
    dut.clk = 0; dut.eval();wave_Trace_once();
    
    if(!dut.rootp->mem_wen) dut.rootp->mem_data = memory_read();  //读内存

    dut.clk = 1; dut.eval();wave_Trace_once();
    clk_cnt++;
}

static void reset(int n) {
    dut.rst = 1;
    while (n -- > 0) single_cycle();
    dut.rst = 0;
}

void cpu_reset(int n, int argc, char **argv){
    if(argv != NULL) wave_Trace_init(argc, argv);
    
    reset(n); 
    clk_cnt = 0;
}

void cpu_value_update(void){
    cpu.pc = dut.rootp->top__DOT__cpu__DOT__pc__DOT__pc;   
    if(!dut.rootp->top__DOT__cpu__DOT__RegWr) return;
    uint32_t rd_iddr = BITS(dut.rootp->inst, 11, 7); //(dut.rootp->inst >> 7) & 0x1f;
    if(rd_iddr != 0) cpu.gpr[rd_iddr] = dut.rootp->top__DOT__cpu__DOT__reg_file__DOT__rf__DOT__rf.m_storage[rd_iddr];
}

char itrace_buf[256];
void itrace_catch(){
    #ifdef ITRACE
    char* p = itrace_buf;

    uint8_t* inst = (uint8_t*)&dut.inst;
    p += snprintf(p, sizeof(itrace_buf),  "0x%08x: ", cpu.pc);
    for(int i = 3; i >= 0; i--){
        p += snprintf(p, 4, "%02x ", inst[i]);
    }
    disassemble(p, itrace_buf + sizeof(itrace_buf) - p, cpu.pc, (uint8_t*)&dut.inst, 4);

    printf("%s\n", itrace_buf);
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

void check_special_inst(void){
    switch(dut.inst){
        case 0x00000000: npc_trap(1);   break; // ecall
        case 0xffffffff: npc_trap(1);   break; // bad trap
        case 0x00008067: is_ret = true;     break; // ret
        default: break;
    }
}

void difftest_step(vaddr_t pc, vaddr_t npc);
static void execute(uint64_t n){
    for(;n > 0; n--){
        // nvboard_update();
        dut.inst = ram_read(cpu.pc, 4);                         //取指

        // if(cpu.pc == 0x80000a5c) printf("0x%08x\n",dut.inst);

        single_cycle();                                                     //单周期执行

        if(dut.rootp->mem_wen) memory_write();          //写内存

        itrace_catch();

        cpu_value_update();          //更新寄存器
        
        difftest_step(cpu.pc, dut.rootp->top__DOT__cpu__DOT__pc__DOT__pc);
        
        check_special_inst();       //检查特殊指令
        func_called_detect();

        if (npc_state.state != NPC_RUNNING) break;
    }
}

int npc_trap (int ra){
    npc_state.state = NPC_END;
    printf("ra: %d\n", ra);
    if(ra == 0) printf("\033[1;32mHit good trap\033[0m\n");
    else printf("\033[1;31mHit bad trap\033[0m\n");
    wave_Trace_close(); 
    return clk_cnt;
}

void cpu_exec(uint64_t n){
    switch (npc_state.state) {
        case NPC_END: case NPC_ABORT:
            printf("Simulation has completed, enter r to restart program\n");
            return;
        default: npc_state.state = NPC_RUNNING;
    }

    // if(is_sim_complete) {
    //     printf("Simulation has completed, enter r to restart program\n");
    //     return;
    // }

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
