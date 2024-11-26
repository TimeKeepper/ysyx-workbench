#include "utils.h"
#include <common.h>
#include <cpu/cpu.h>
#include <sdb/sdb.h>

void engine_start(int argc, char **argv) {
    cpu_reset(20); 

    sdb_mainloop();
}
