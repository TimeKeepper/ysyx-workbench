#include "utils.h"
#include "cpu/cpu.h"

uint64_t IFU_pc = 0, LSU_pc = 0, ALU_pc = 0;

extern "C" void IFU_finished() {
    IFU_pc++;
}

extern "C" void LSU_finished() {
    LSU_pc++;
}

extern "C" void ALU_finished() {
    ALU_pc++;
}
