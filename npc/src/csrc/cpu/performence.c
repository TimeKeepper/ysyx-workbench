#include "utils.h"
#include "cpu/cpu.h"

uint64_t IFU_pc = 0, LSU_pc = 0, EXU_pc = 0;

extern "C" void IFU_finished() {
    Log("IFU finished");
    IFU_pc++;
}

extern "C" void LSU_finished() {
    LSU_pc++;
}

extern "C" void EXU_finished() {
    EXU_pc++;
}
