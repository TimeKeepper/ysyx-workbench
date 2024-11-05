#include "utils.h"
#include "cpu/cpu.h"

uint64_t IFU_pc = 0, LSU_pc = 0, ALU_pc = 0;

uint64_t i_CSR = 0, i_LS = 0, i_Cal = 0;

extern "C" void IFU_finished() {
    IFU_pc++;
}

extern "C" void LSU_finished() {
    LSU_pc++;
}

extern "C" void ALU_finished() {
    ALU_pc++;
}

extern "C" void IDU_finished(uint32_t iType) {
    switch (iType) {
        case 0: i_LS ++; break;
        case 1: i_CSR ++; break;
        case 2: i_Cal ++; break;
    }
}
