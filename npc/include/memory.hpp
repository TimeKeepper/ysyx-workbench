#ifndef __MEMORY_HPP__
#define __MEMORY_HPP__

#include "common.hpp"
#include <sys/types.h>
#include <utils.hpp>

class Memory {
    private:
        uint8_t* memory;
        size_t size;

        void len_require(int len);
    public:
        Memory(size_t size);
        ~Memory();

        uint8_t* get_memory() { return this->memory; }
        word_t read(uint32_t addr, int len);
        void write(uint32_t addr, int len, word_t data);
};

#endif
