#include "common.h"
#include <stdbool.h>
#include <stdint.h>

typedef struct {
    word_t inst;
    bool map_hit;
    bool cache_hit;
} Icache_return;
