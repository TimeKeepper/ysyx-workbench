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

#ifdef __cplusplus
#include <list>
#include <cassert>
#include <algorithm>
#include <vector>

struct CacheLine {
    bool valid;
    uint32_t tag;
    std::vector<uint32_t> inst;

    CacheLine() : CacheLine(CONFIG_ICache_Block_Size) {}

    CacheLine(uint32_t block_size) {
        assert(block_size % 4 == 0 && block_size >= 4);
        valid = false;
        tag = 0;
        inst.resize(block_size / 4);
        std::fill(inst.begin(), inst.end(), 0);
    }
};

class Icache_LRU {
    private:
        uint32_t begin;
        uint32_t end;
        uint32_t way;
        uint32_t set;
        uint32_t block_size;
        uint32_t offset_bits;
        uint32_t set_bits;
        uint32_t tag_bits;
        
    public:
        std::vector<std::vector<CacheLine>> cache;
        std::vector<std::list<uint32_t>> lru; 

        void init(uint32_t begin, uint32_t end, uint32_t way, uint32_t set, uint32_t block_size = 4);
        
        Icache_return fetch(vaddr_t addr, uint32_t len);
    
        void print_cache();
};

class Icache_PLRU {
    private:
        uint32_t begin;
        uint32_t end;
        uint32_t way;
        uint32_t depth; // depth of the binary tree
        uint32_t set;
        uint32_t block_size;
        uint32_t offset_bits;
        uint32_t set_bits;
        uint32_t tag_bits;

    public:
        std::vector<std::vector<CacheLine>> cache;
        std::vector<bool> lru_bits;

        void init(uint32_t begin, uint32_t end, uint32_t way, uint32_t set, uint32_t block_size = 4);

        Icache_return fetch(vaddr_t addr, uint32_t len);

        void print_cache();
};

extern Icache_LRU icache;
#endif

#endif