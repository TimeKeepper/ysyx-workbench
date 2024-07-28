#ifndef __CPU_CPU_H__
#define __CPU_CPU_H__

#ifdef DEFINE_NPC
#include <Vtop.h>
#include "Vtop__Dpi.h"
#else
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
void cpu_reset(int n, int argc, char **argv);
void wave_Trace_init(int argc, char **argv);
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

#endif