#include <memory.hpp>

Memory::Memory(size_t size, int endian) : endian(endian) {
    this->size = size;
    this->memory = new uint8_t[size];
}

Memory::~Memory() {
    delete[] this->memory;
}

void Memory::len_require(int len) {
    Assert(len == 1 || len == 2 || len == 4, "len must be 1, 2 or 4");
}

word_t Memory::read(uint32_t addr, int len) {
    this->len_require(len);

    word_t data = 0;
    for (int i = 0; i < len; i++) {
        if(this->endian == Big_endian) data |= this->memory[addr + i] << (i * 8);
        else data |= this->memory[addr + i] << ((len - i - 1) * 8);
    }
    return data;
}

void Memory::write(uint32_t addr, int len, word_t data) {
    this->len_require(len);

    for (int i = 0; i < len; i++) {
        this->memory[addr + i] = (data >> (i * 8)) & 0xff;
    }
}
