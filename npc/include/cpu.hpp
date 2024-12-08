#ifndef __CPU_CPU_H__
#define __CPU_CPU_H__

#ifdef PLATFORM_NPC
#include <Vtop.h>
#include "Vtop__Dpi.h"
#elif defined (PLATFORM_YSYXSOC)
#include <VysyxSoCFull.h>
#include "VysyxSoCFull__Dpi.h"
#endif

#include <utils.hpp>
#include <nvboard.h>

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
} Riscv_CPU_State;

extern std::map<uint32_t, std::string> csr_key;

const char* gpr_id2name(int id);
const char* csr_id2name(int id);

#endif