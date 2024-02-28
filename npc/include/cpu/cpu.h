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

bool cpu_exec(uint64_t n);
void cpu_reset(int n, int argc, char **argv);

extern CPU_State cpu;

#endif