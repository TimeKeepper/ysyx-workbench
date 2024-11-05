#ifndef __CPU_CPU_H__
#define __CPU_CPU_H__

#ifdef PLATFORM_NPC
#include <Vtop.h>
#include "Vtop__Dpi.h"
#elif defined (PLATFORM_YSYXSOC)
#include <VysyxSoCFull.h>
#include "VysyxSoCFull__Dpi.h"
#endif

#include <common.h>
#include <nvboard.h>
#include "verilated.h"
#include "verilated_vcd_c.h"

#define ADDR_MSTATUS 0x300
#define ADDR_MTVEC 0x305
#define ADDR_MSCRATCH 0x340
#define ADDR_MEPC 0x341
#define ADDR_MCAUSE 0x342
#define ADDR_MVENDORID 0xF11
#define ADDR_MARCHID 0xF12

const int sregs_iddr[] = {
  ADDR_MSTATUS, ADDR_MTVEC, ADDR_MEPC, ADDR_MCAUSE, ADDR_MSCRATCH
};

typedef struct {
    word_t gpr[32];
    vaddr_t pc;
    word_t sr[4096];
} CPU_State;

void clk_exec(uint64_t n);
void cpu_exec(uint64_t n);
void cpu_reset(int n);
void Init_wavetrace(int argc, char **argv);
void wave_Trace_once();
void wave_Trace_close();
char* reg_id2name(int id);
int reg_name2id(char *reg_name);
void isa_reg_display(char *reg_name);
extern "C" void init_disasm(const char *triple);
extern "C" void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
void init_difftest(char *ref_so_file, long img_size, int port);
void difftest_step(vaddr_t pc, vaddr_t npc);
int npc_trap (int ra);

extern CPU_State cpu;

extern uint64_t IFU_pc, LSU_pc, ALU_pc;
extern uint64_t i_CSR,  i_LS,  i_Cal;
extern uint64_t clk_cnt, inst_cnt;
extern bool is_itrace_printf;

#endif