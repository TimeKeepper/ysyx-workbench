#ifndef __MEMORY_HPP__
#define __MEMORY_HPP__

#include "common.hpp"
#include <cmath>
#include <sys/types.h>
#include <utils.hpp>

class Memory {
    private:
        uint8_t* memory;
        int endian;

        void len_require(int len);
    public:
        enum {Big_endian, Little_endian};

        uint32_t base;
        size_t size;
        Memory(uint32_t base, size_t size, int endian = Big_endian);
        ~Memory();

        uint8_t* get_memory() { return this->memory; }
        word_t read(uint32_t addr, int len);
        word_t read_WithBias(uint32_t addr, int len);
        void write(uint32_t addr, int len, word_t data);
        bool match(uint32_t addr);
};

struct CacheLine {
    uint32_t tag;
    word_t inst;
    bool valid;
};

#ifdef CONFIG_PLATFORM_YSYXSOC
const uint32_t ICACHE_BEGIN = CONFIG_SDRAM_BASE;
const uint32_t ICACHE_END = CONFIG_SDRAM_BASE + CONFIG_SDRAM_SIZE;
#elif defined (CONFIG_PLATFORM_NPC)
const uint32_t ICACHE_BEGIN = CONFIG_LOAD_MEMORY_BASE;
const uint32_t ICACHE_END = CONFIG_LOAD_MEMORY_BASE + CONFIG_LOAD_MEMORY_SIZE;
#endif

const uint32_t ICACHE_VALID_BITS = ceil(std::log2((double)(ICACHE_END - ICACHE_BEGIN)));
const uint32_t ICACHE_OFFSET_BITS = ceil(std::log2((double)CONFIG_ICache_Block_Size));
const uint32_t ICACHE_SET_BITS = ceil(log2((double)CONFIG_ICache_Set));
const uint32_t ICACHE_TAG_BITS = ICACHE_VALID_BITS - ICACHE_OFFSET_BITS - ICACHE_SET_BITS;

#endif
