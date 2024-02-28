#include <common.h>
#include <cpu/cpu.h>
#include <sdb/sdb.h>

void engine_start(int argc, char **argv) {
    cpu_reset(10, argc, argv); 

    sdb_mainloop();
}
