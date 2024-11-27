#include <memory.hpp>

Memory::Memory(size_t size) {
    this->size = size;
    this->memory = new uint8_t[size];
}

Memory::~Memory() {
    delete[] this->memory;
}

void Memory::len_require(int len) {
    Assert(len == 1 || len == 2 || len == 4, "len must be 1, 2 or 4");
}

word_t Memory::read(void *addr, int len) {
    this->len_require(len);

    word_t data = 0;
    for (int i = 0; i < len; i++) {
        data |= this->memory[(uint64_t)addr + i] << (i * 8);
    }
    return data;
}

void Memory::write(void *addr, int len, word_t data) {
    this->len_require(len);

    for (int i = 0; i < len; i++) {
        this->memory[(uint64_t)addr + i] = (data >> (i * 8)) & 0xff;
    }
}
