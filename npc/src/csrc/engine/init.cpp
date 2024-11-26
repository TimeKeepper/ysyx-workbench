#include "utils.hpp"
#include <common.hpp>
#include <cpu/cpu.hpp>
#include <sdb/sdb.hpp>

void engine_start(int argc, char **argv) {
    cpu_reset(20); 

    sdb_mainloop();
}
