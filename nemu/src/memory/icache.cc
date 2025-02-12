#include "common.h"
#include <memory/icache.h>

extern "C" Icache_return icache_fetch(vaddr_t addr) {
    Icache_return ret;
    ret.inst = 0;
    ret.map_hit = false;
    ret.cache_hit = false;
    return ret;
}
