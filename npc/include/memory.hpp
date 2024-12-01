#ifndef __MEMORY_HPP__
#define __MEMORY_HPP__

#include "common.hpp"
#include <sys/types.h>
#include <utils.hpp>

class Memory {
    private:
        uint8_t* memory;
        uint32_t base;
        size_t size;
        int endian;

        void len_require(int len);
    public:
        enum {Big_endian, Little_endian};

        Memory(uint32_t base, size_t size, int endian = Big_endian);
        ~Memory();

        uint8_t* get_memory() { return this->memory; }
        word_t read(uint32_t addr, int len);
        void write(uint32_t addr, int len, word_t data);
        bool match(uint32_t addr);
};

#endif
