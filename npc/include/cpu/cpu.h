#ifndef __CPU_CPU_H__
#define __CPU_CPU_H__

#include <common.h>
#include <nvboard.h>
#include <Vtop.h>
#include "verilated.h"
#include "verilated_vcd_c.h"
#include "Vtop__Dpi.h"

typedef struct {
    word_t gpr[32];
    vaddr_t pc;
} CPU_State;

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

extern CPU_State cpu;

#endif