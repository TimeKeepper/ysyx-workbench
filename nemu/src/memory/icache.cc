#include <cstdint>
#include <iomanip>
#include <stdio.h>

#include "utils.h"
extern "C" {
    #include "common.h"
    #include "memory/paddr.h"
}

#include <cmath>
#include <list>
#include <vector>
#include <iostream>
#include <memory/icache.hpp>

void Icache_LRU::init(uint32_t begin, uint32_t end, uint32_t way, uint32_t set, uint32_t block_size) {
        this->begin = begin;
        this->end = end;
        this->way = way;
        this->set = set;
        this->block_size = block_size;
        this->offset_bits = ceil(std::log2((double)block_size));
        this->set_bits = ceil(log2((double)set));

        uint32_t valid_bits = ceil(std::log2((double)(end - begin)));
        this->tag_bits = valid_bits - offset_bits - set_bits;

        cache.resize(set, std::vector<CacheLine>(way, CacheLine(block_size)));
        lru.resize(set, std::list<uint32_t>());
        for(uint32_t i = 0; i < set; ++i) {
            for(uint32_t j = 0; j < way; ++j) {
                lru[i].push_front(j);
            }
        }
    }
    
Icache_return Icache_LRU::fetch(vaddr_t addr, uint32_t len) {
    Icache_return result;
    result.inst = 0;
    result.map_hit = (addr >= begin) && (addr < end);
    result.cache_hit = false;

    if(!result.map_hit) {
        // 地址不在映射范围内, 从内存中加载指令
        result.inst = paddr_read(addr, len);
        return result;
    }

    uint32_t offset = addr & ((1 << offset_bits) - 1);
    uint32_t set_idx = (addr >> offset_bits) & ((1 << set_bits) - 1);
    uint32_t tag = (addr >> (offset_bits + set_bits)) & ((1 << tag_bits) - 1);
    
    // 检查命中
    for(uint32_t w = 0; w < way; ++w) {
        if(cache[set_idx][w].valid && cache[set_idx][w].tag == tag) {
            // 更新LRU列表
            lru[set_idx].remove(w);
            lru[set_idx].push_back(w);
            result.inst = cache[set_idx][w].inst[offset / 4];
            result.cache_hit = true;
            return result;
        }
    }

    // 缓存未命中，从内存中加载指令
    // 替换策略：替换LRU列表末尾的缓存行
    int replace_way = lru[set_idx].front();
    lru[set_idx].pop_front();
    lru[set_idx].push_back(replace_way);

    // Calculate base address aligned to block size
    vaddr_t block_addr = addr & ~((1 << offset_bits) - 1);
    
    // Load entire cache block at once
    auto& block_data = cache[set_idx][replace_way].inst;
    for (uint32_t i = 0; i < block_size / 4; ++i) {
        block_data[i] = paddr_read(block_addr + i * 4, 4);
    }
    
    // Get the requested instruction
    result.inst = block_data[offset / 4];

    cache[set_idx][replace_way].tag = tag;
    // cache[set_idx][replace_way].inst = replace_data
    cache[set_idx][replace_way].valid = true;

    return result;
}

void Icache_LRU::print_cache() {
    for(uint32_t i = 0; i < set; ++i) {
        std::cout << ANSI_FG_BLUE << "Set " << i << ": " << std::endl;
        for(uint32_t j = 0; j < way; ++j) {
            std::cout << ANSI_FG_CYAN"valid " << (cache[i][j].valid ? ANSI_FG_GREEN"true" : ANSI_FG_RED"false") << '\t'
                << ANSI_FG_CYAN"tag[" << tag_bits << "] " <<  std::hex << ANSI_FG_BLUE"0x" << cache[i][j].tag << '\t'
                << ANSI_FG_CYAN"data " << ANSI_FG_BLUE;

            for(uint32_t k : cache[i][j].inst) {
                std::cout << std::hex << std::setw(8) << std::setfill('0') << k << " ";
            }

            // std::cout << cache[i][j].inst;
            std::cout << std::dec << '\t'
                << ANSI_NONE << std::endl;
        }
        std::cout << std::endl;
    }
}

Icache_LRU icache;

extern "C" void Icache_init(paddr_t begin, paddr_t end, uint32_t way, uint32_t set, uint32_t block_size) {
    icache.init(begin, end, way, set, block_size);
}

extern "C" Icache_return icache_fetch(vaddr_t addr, uint32_t len) {
    return icache.fetch(addr, len);
}

extern "C" void Icache_print() {
    icache.print_cache();
}
