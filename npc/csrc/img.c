#include <img.h>

uint32_t inst_ram[RAM_SIZE];

void inst_ram_init(void) {
  for(int i = 0; i < RAM_SIZE; i++){
    inst_ram[i] = 0b00000000000100001000000010010011;
  }
  inst_ram[100] = 0x00100073;
}

uint32_t inst_ram_read(uint32_t addr){
    addr %= RAM_SIZE;
    addr /= 4;
    return inst_ram[addr];
}