#ifndef __IMG_H__
#define __IMG_H__

#include <cstdint>
#define RAM_SIZE 1024

void inst_ram_init(void);
uint32_t inst_ram_read(uint32_t addr);

#endif