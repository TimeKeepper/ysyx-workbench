#ifndef __MEMORY_ICACHE_H__
#define __MEMORY_ICACHE_H__

#include "common.h"
#include <stdbool.h>
#include <stdint.h>

typedef struct {
    word_t inst;
    bool map_hit;
    bool cache_hit;
} Icache_return;

#endif