#include "utils.h"
#include "cpu/cpu.h"

uint64_t IFU_pc = 0, LSU_pc = 0, ALU_pc = 0;

extern "C" void IFU_finished() {
    Log("IFU finished");
    IFU_pc++;
}

extern "C" void LSU_finished() {
    Log("LSU finished");
    LSU_pc++;
}

extern "C" void ALU_finished() {
    Log("ALU finished");
    ALU_pc++;
}
