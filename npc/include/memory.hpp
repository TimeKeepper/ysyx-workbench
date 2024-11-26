#ifndef __MEMORY_HPP__
#define __MEMORY_HPP__

#include "common.hpp"
#include <utils.hpp>

class Memory {
    private:
        uint8_t* memory;
        size_t size;

        void len_require(int len);
    public:
        Memory(size_t size);
        ~Memory();

        word_t read(void *addr, int len);
        void write(void *addr, int len, word_t data);
};

#endif
