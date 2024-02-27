#include <img.h>
#include <stdio.h>

uint32_t inst_ram_read(uint32_t addr){
    for(int i = 0; i < 10; i++){
        printf("%d\n", inst_ram[i]);
    }
    addr %= RAM_SIZE;
    addr /= 4;
    return inst_ram[addr];
}