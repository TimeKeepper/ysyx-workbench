#ifndef __IMG_H__
#define __IMG_H__

#include <gelf.h>

#define RAM_SIZE 1024

uint32_t inst_ram_read(uint32_t addr);

static uint32_t inst_ram[RAM_SIZE];

#endif