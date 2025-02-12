#include <iomanip>

#include "utils.h"
extern "C" {
    #include "common.h"
    #include <memory/icache.h>
    #include "memory/paddr.h"
}

#include <cmath>
#include <list>
#include <vector>
#include <iostream>


struct CacheLine {
    uint32_t tag;
    word_t inst;
    bool valid;
};

// 指令缓存类
class Icache {
private:
    uint32_t begin;
    uint32_t end;
    int way;
    int set;
    int line_size;
    int offset_bits;
    int index_bits;
    int set_bits;
    int tag_bits;
    std::vector<std::vector<CacheLine>> cache;
    std::vector<std::list<int>> lru; // 每个组的LRU列表

public:
    // 初始化缓存
    void init(uint32_t begin, uint32_t end, int way, int set, int line_size = 4) {
        this->begin = begin;
        this->end = end;
        this->way = way;
        this->set = set;
        this->line_size = line_size;
        this->offset_bits = ceil(std::log2((double)line_size));
        this->index_bits = ceil(std::log2((double)way));
        this->set_bits = ceil(log2((double)set));

        uint32_t valid_bits = ceil(std::log2((double)(end - begin)));
        this->tag_bits = valid_bits - offset_bits - index_bits - set_bits;

        printf("offset_bits: %d, index_bits %d, set_bits: %d, valid_bits: %d tag_bits: %d\n", offset_bits, index_bits, set_bits, valid_bits, tag_bits);
        printf("size: %08x", end - begin);

        cache.resize(set, std::vector<CacheLine>(way));
        lru.resize(set, std::list<int>());
        for(int i = 0; i < set; ++i) {
            for(int j = 0; j < way; ++j) {
                cache[i][j].valid = false;
                cache[i][j].tag = 0;
                cache[i][j].inst = 0;
                lru[i].push_back(j);
            }
        }
    }
    
    // 获取指令
    Icache_return fetch(vaddr_t addr, int len) {
        Icache_return result;
        result.inst = 0;
        result.map_hit = (addr >= begin) && (addr < end);
        result.cache_hit = false;

        if(!result.map_hit) {
            // 地址不在映射范围内, 从内存中加载指令
            result.inst = paddr_read(addr, len);
            return result;
        }

        // uint32_t offset = addr & ((1 << offset_bits) - 1);
        uint32_t set_idx = (addr >> offset_bits) & ((1 << set_bits) - 1);
        uint32_t tag = (addr >> (offset_bits + set_bits)) & ((1 << tag_bits) - 1);

        // 检查命中
        for(int w = 0; w < way; ++w) {
            if(cache[set_idx][w].valid && cache[set_idx][w].tag == tag) {
                // 更新LRU列表
                lru[set_idx].remove(w);
                lru[set_idx].push_front(w);
                result.inst = cache[set_idx][w].inst;
                result.cache_hit = true;
                return result;
            }
        }

        // 缓存未命中，从内存中加载指令
        result.inst = paddr_read(addr, len);

        // 替换策略：替换LRU列表末尾的缓存行
        int replace_way = lru[set_idx].back();
        lru[set_idx].pop_back();
        lru[set_idx].push_front(replace_way);

        cache[set_idx][replace_way].tag = tag;
        cache[set_idx][replace_way].inst = result.inst;
        cache[set_idx][replace_way].valid = true;

        return result;
    }

    void print_cache() {
        for(int i = 0; i < set; ++i) {
            std::cout << ANSI_FG_BLUE << "Set " << i << ": " << std::endl;
            for(int j = 0; j < way; ++j) {
                std::cout << ANSI_FG_CYAN"valid " << (cache[i][j].valid ? ANSI_FG_GREEN"true" : ANSI_FG_RED"false") << '\t'
                    << ANSI_FG_CYAN"tag[" << tag_bits << "] " <<  std::hex << ANSI_FG_BLUE"0x" << cache[i][j].tag << '\t'
                    << ANSI_FG_CYAN"data " << std::hex << ANSI_FG_BLUE << "0x" << std::setw(8) << std::setfill('0') << cache[i][j].inst << '\t'
                    << ANSI_NONE << std::endl;
            }
            std::cout << std::endl;
        }
    }
};

Icache icache;

extern "C" void Icache_init(paddr_t begin, paddr_t end, int way, int set) {
    icache.init(begin, end, way, set);
}

extern "C" Icache_return icache_fetch(vaddr_t addr, int len) {
    return icache.fetch(addr, len);
}

extern "C" void Icache_print() {
    icache.print_cache();
}
