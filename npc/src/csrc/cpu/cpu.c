#include "Vtop___024root.h"
#include "verilated_vcd_c.h"
#include <cpu/cpu.h>
#include <csignal>
#include <cstdint>
#include <memory/paddr.h>
#include <utils.h>
#include <sdb/sdb.h>

TOP_NAME dut;
uint32_t clk_cnt = 0;

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

struct Helper {
    Helper() {
        Verilated::traceEverOn(true);
    }
};
Helper helper;
const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
VerilatedVcdC* tfp = new VerilatedVcdC;

void wave_Trace_init(int argc, char **argv){
    #ifdef WAVE_TRACE
    contextp->commandArgs(argc, argv);
    dut.trace(tfp, 99);
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

uint32_t ram_read(uint32_t addr, int len){
    return paddr_read(addr, len);
}

void ram_write(paddr_t addr, int len, word_t data){
    paddr_write(addr, len, data);
}

uint32_t memory_read(void){
    uint32_t Dmem_addr = dut.rootp->Dmem_addr;
    if (!(likely(in_pmem(Dmem_addr)) || (Dmem_addr == RTC_ADDR) || (Dmem_addr == RTC_ADDR + 4))) return 0;
    switch(dut.rootp->Dmemop){
        case 0b000: return ram_read(Dmem_addr,  1);
        case 0b001: return SEXT(ram_read(Dmem_addr,  1), 8);
        case 0b010: return ram_read(Dmem_addr,  2);
        case 0b011: return SEXT(ram_read(Dmem_addr,  2), 16);
        case 0b100: return ram_read(Dmem_addr,  4);
        default: return 0;
    }
}

void memory_write(void){
    uint32_t Dmem_addr = dut.rootp->Dmem_addr;
    if (!((likely(in_pmem(Dmem_addr))) || Dmem_addr == SERIAL_PORT)) return;
    switch(dut.rootp->Dmemop){
        case 0b000:
        case 0b001: ram_write(Dmem_addr,  1, dut.rootp->Dmemdata); break;
        case 0b010:
        case 0b011: ram_write(Dmem_addr,  2, dut.rootp->Dmemdata); break;

        case 0b100: ram_write(Dmem_addr,  4, dut.rootp->Dmemdata); break;
        default: break;
    }
}

static void single_cycle() {
    dut.clk = 0; dut.eval();wave_Trace_once();                  
    
    if(!dut.rootp->Dmem_wen) dut.rootp->Dmem_data = memory_read();  

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

const int sregs_iddr[] = {
  ADDR_MSTATUS, ADDR_MTVEC, ADDR_MEPC, ADDR_MCAUSE, ADDR_MSCRATCH
};

void cpu_value_update(void){
    cpu.pc = dut.rootp->Imem_raddr;   
    cpu.sr[sregs_iddr[0]] = dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__csr_0;
    cpu.sr[sregs_iddr[1]] = dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__csr_5;
    cpu.sr[sregs_iddr[2]] = dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__csr_65;
    cpu.sr[sregs_iddr[3]] = dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__csr_66;
    cpu.sr[sregs_iddr[4]] = dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__csr_64; 

    // if(!dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__RegWr) return;
    uint32_t rd_iddr = BITS(dut.rootp->io_Imem_rdata_bits, 11, 7); //(dut.rootp->inst >> 7) & 0x1f;
    
    switch(rd_iddr) {
        case 0: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_0);  break;
        case 1: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_1);  break;
        case 2: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_2);  break;
        case 3: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_3);  break;
        case 4: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_4);  break;
        case 5: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_5);  break;
        case 6: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_6);  break;
        case 7: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_7);  break;
        case 8: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_8);  break;
        case 9: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_9);  break;
        case 10: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_10); break;
        case 11: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_11); break;
        case 12: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_12); break;
        case 13: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_13); break;
        case 14: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_14); break;
        case 15: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_15); break;
        case 16: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_16); break;
        case 17: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_17); break;
        case 18: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_18); break;
        case 19: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_19); break;
        case 20: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_20); break;
        case 21: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_21); break;
        case 22: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_22); break;
        case 23: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_23); break;
        case 24: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_24); break;
        case 25: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_25); break;
        case 26: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_26); break;
        case 27: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_27); break;
        case 28: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_28); break;
        case 29: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_29); break;
        case 30: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_30); break;
        case 31: cpu.gpr[rd_iddr] = (dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__gpr_31); break;
        default: break;
    }
}

char itrace_buf[256];
void itrace_catch(bool is_printf){
    #ifdef ITRACE
    char* p = itrace_buf;

    uint8_t* inst = (uint8_t*)&dut.Imem_rdata;
    p += snprintf(p, sizeof(itrace_buf),  "0x%08x: ", cpu.pc);
    for(int i = 3; i >= 0; i--){
        p += snprintf(p, 4, "%02x ", inst[i]);
    }
    disassemble(p, itrace_buf + sizeof(itrace_buf) - p, cpu.pc, (uint8_t*)&dut.Imem_rdata, 4);

    instr_buf_push(itrace_buf);

    if(is_printf) printf("%s\n", itrace_buf);
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

void watchpoint_catch(void){
    #ifdef CONFIG_WATCHPOINT
    wp_Value_Update();
    WP* wp;
    for(int i = 0; (wp = get_Changed_wp(i)) != NULL; i++){
        printf("Watchpoint %d: " ANSI_FMT("%s\n", ANSI_FG_BLUE), wp->NO, wp->expr);
        printf(ANSI_FMT("Old value" , ANSI_FG_YELLOW)  " = 0x%08x\n", wp->last_time_Value);
        printf(ANSI_FMT("New value" , ANSI_FG_GREEN) " = 0x%08x\n", wp->value);
        if(npc_state.state != NPC_END) npc_state.state = NPC_STOP;//如果在npc停止的情况下修改state，就会导致报错,因为会导致检查trap的时候无法通过NPC_END的判断
    }
    #endif
}

void check_special_inst(void){
    switch(dut.io_Imem_rdata_bits){
        case 0x00000000: npc_trap(1);   break; // ecall
        case 0xffffffff: npc_trap(1);   break; // bad trap
        case 0x00008067: is_ret = true;     break; // ret
        default: break;
    }
}

void difftest_step(vaddr_t pc, vaddr_t npc);
static void execute(uint64_t n){
    bool is_itrace = (n < MAX_INST_TO_PRINT);
    for(;n > 0; n--){
        // nvboard_update();
        dut.io_Imem_rdata_bits = ram_read(dut.Imem_raddr, 4); 

        single_cycle();         

            if(dut.rootp->Dmem_wen) memory_write();          //写内存

            itrace_catch(is_itrace);

            cpu_value_update();          //更新寄存器
            
            watchpoint_catch();          //检查watchpoint

            if(dut.inst_comp) difftest_step(cpu.pc, dut.rootp->top__DOT__npc__DOT__riscv_cpu__DOT__REG__DOT__pc);
            
            check_special_inst();       //检查特殊指令
            func_called_detect();                                            //单周期执行

        if (npc_state.state != NPC_RUNNING) break;
    }
}

int npc_trap (int ra){
    npc_state.state = NPC_END;
    printf("ra: %d\n", ra);
    if(ra == 0) printf("\033[1;32mHit good trap\033[0m\n");
    else printf("\033[1;31mHit bad trap\033[0m\n");
    wave_Trace_once();
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
