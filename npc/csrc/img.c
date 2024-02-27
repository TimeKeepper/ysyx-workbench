#include <img.h>

uint32_t inst_ram_read(uint32_t addr){
    addr %= RAM_SIZE;
    addr /= 4;
    return inst_ram[addr];
}