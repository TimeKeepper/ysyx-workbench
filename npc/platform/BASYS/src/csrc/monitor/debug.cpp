#include <utils.hpp>

extern "C" void bram_ui_read(int raddr, int *rdata){
    *rdata = 0x000;
}
