#include "utils.h"
#include "cpu/cpu.h"

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
