#include "sdb/cmd.h"
#include "utils.h"
#include <cassert>
#include <common.h>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <memory/paddr.h>
#include <memory/host.h>
#include <stdio.h>

#define PG_ALIGN __attribute((aligned(4096)))

static uint8_t psram[CONFIG_PSRAM_SIZE] PG_ALIGN = {};

static uint8_t sdram[CONFIG_SDRAM_SIZE] PG_ALIGN = {};

static uint8_t mrom[CONFIG_MROM_SIZE] PG_ALIGN = {};

static uint8_t flash[CONFIG_FLASH_SIZE] PG_ALIGN = {};

uint8_t* guest_to_host_psram(paddr_t paddr) { return psram + paddr - CONFIG_PSRAM_BASE; }
paddr_t host_to_guest_psram(uint8_t *haddr) { return haddr - psram + CONFIG_PSRAM_BASE; }

uint8_t* guest_to_host_sdram(paddr_t paddr) { return sdram + paddr - CONFIG_FLASH_BASE; }
paddr_t host_to_guest_sdram(uint8_t *haddr) { return haddr - sdram + CONFIG_FLASH_SIZE; }

uint8_t* guest_to_host_mrom(paddr_t paddr) { return mrom + paddr - CONFIG_MROM_BASE; }
paddr_t host_to_guest_mrom(uint8_t *haddr) { return haddr - mrom + CONFIG_MROM_SIZE; }

uint8_t* guest_to_host_flash(paddr_t paddr) { return flash + paddr - CONFIG_FLASH_BASE; }
paddr_t host_to_guest_flash(uint8_t *haddr) { return haddr - flash + CONFIG_FLASH_SIZE; }

uint8_t* guest_to_host(paddr_t paddr) { 
    if(in_psram(paddr)) return guest_to_host_psram(paddr);
    else if(in_sdram(paddr)) return guest_to_host_sdram(paddr);
    else if(in_mrom(paddr)) return guest_to_host_mrom(paddr);
    else if(in_flash(paddr)) return guest_to_host_flash(paddr);
    else return nullptr;
}

#define CODE_MEMORY flash

static const uint32_t img [] = {
  0x00000513,  // li a0 0
  0x00150513,  // add a0 a0 1
  0x00a12023,  // sw a0 0(sp)
  0xff9fffef,  // jmp 0x80000004
};

void mem_random_set(void){
  memset(psram, rand(), CONFIG_PSRAM_SIZE);
  memset(flash, rand(), CONFIG_FLASH_SIZE);
  memset(mrom,  rand(), CONFIG_MROM_SIZE);
  memset(sdram, rand(), CONFIG_SDRAM_SIZE);
}

void init_mem() {
    mem_random_set();
    memcpy(CODE_MEMORY, img, sizeof(img));
    Log("SRAM memory area \t [" "0x%08x" ", " "0x%08x" "]", SRAM_LEFT, SRAM_RIGHT);
    Log("MROM memory area \t [" "0x%08x" ", " "0x%08x" "]", MROM_LEFT, MROM_RIGHT);
    Log("FLASH memory area \t [" "0x%08x" ", " "0x%08x" "]", FLASH_LEFT, FLASH_RIGHT);
    Log("PSRAM memory area \t [" "0x%08x" ", " "0x%08x" "]", PSRAM_LEFT, PSRAM_RIGHT);
}

static word_t psram_read(paddr_t addr, int len) {
    word_t ret = host_read(guest_to_host_psram(addr), len);
    return ret;
}

static void psram_write(paddr_t addr, int len, word_t data) {
    host_write(guest_to_host_psram(addr), len, data);
}

static word_t sdram_read(paddr_t addr, int len) {
    word_t ret = host_read(guest_to_host_sdram(addr), len);
    return ret;
}

static void sdram_write(paddr_t addr, int len, word_t data) {
    host_write(guest_to_host_sdram(addr), len, data);
}

static word_t mrom_read(paddr_t addr) {
    word_t ret = host_read(guest_to_host_mrom(addr & ~0x3u), 4);
    return ret;
}

static word_t flash_read(paddr_t addr) {
    word_t ret = host_read(guest_to_host_flash(addr & ~0x3u), 4);
    return ret;
}

static void out_of_bound(paddr_t addr) {
    printf("address =  0x%08x  is out of bound", addr);
    npc_state.state = NPC_ABORT;
}

void difftest_skip_ref();

word_t paddr_read(paddr_t addr, int len) {
    if (likely(in_psram(addr))) return psram_read(addr, len);
    else if (likely(in_sdram(addr))) return sdram_read(addr, len);
    else if(likely(in_mrom(addr))) return mrom_read(addr);
    else if(likely(in_flash(addr))) return flash_read(addr);
    else if(addr == RTC_ADDR ) {difftest_skip_ref(); return get_time();}
    else if(addr == RTC_ADDR + 4) { difftest_skip_ref(); return get_time() >> 32;}
    out_of_bound(addr);
    return 0;
}

void paddr_write(paddr_t addr, int len, word_t data) {
    if (likely(in_psram(addr))) {psram_write(addr, len, data); return; }
    else if(in_sdram(addr)) {sdram_write(addr, len, data); return; }
    out_of_bound(addr);
}

uint8_t* get_pmem(void) { //获取存放程序的内存节
    return psram;
}

uint8_t* get_flash(void) { //获取存放flash的内存节
    return flash;
}

extern "C" void flash_read(int32_t addr, int32_t *data) {
    int32_t data_tmp;
    uint32_t addr_tmp = (addr & ~0x3u);
    data_tmp = (host_read(flash + addr_tmp + 0, 1) << 24) | \
               (host_read(flash + addr_tmp + 1, 1) << 16) | \
               (host_read(flash + addr_tmp + 2, 1) << 8) | \
               (host_read(flash + addr_tmp + 3, 1) << 0);
    *data = data_tmp;
    // printf("addr: 0x%8x data: 0x%8x", addr, *data);
    }
// extern "C" void flash_read(int32_t addr, int32_t *data) {*data = host_read((mrom + addr), 4); printf("flash_read: addr = 0x%08x, data = 0x%08x\n", addr, *data);}
extern "C" void mrom_read(int32_t addr, int32_t *data) { *data = paddr_read(addr, 4); }

extern "C" void psram_write(int32_t waddr, int32_t wdata, int32_t wlen) { 
    uint32_t wdata_tmp;
    switch(wlen){
        case 2: wdata_tmp = (wdata) >> 24; break;
        case 4: wdata_tmp = (wdata) >> 16; break;
        case 8: wdata_tmp = (wdata); break;
        default: wdata_tmp = (wdata); break;
    }
    host_write(psram + waddr, wlen / 2, wdata_tmp); 
    // printf("host_write: waddr = 0x%08x, wdata = 0x%08x, wlen = %d\n", waddr, wdata_tmp, wlen); 
} 

extern "C" void psram_read(int32_t raddr, int32_t *rdata) { 
    *rdata = host_read(psram + raddr, 4); 
}//printf("host_read: raddr = 0x%08x, rdata = 0x%08x\n", raddr, *rdata);

extern "C" void sdram_read(int32_t raddr, uint16_t *rdata) {
    uint32_t rdata_tmp = host_read(sdram + (raddr & ~0x3u), 4);
    *rdata = host_read(sdram + raddr, 2);
    extern uint64_t clk_cnt;
    Log("sdram_read: raddr = 0x%08x, rdata_tmp = 0x%08x, rdata = 0x%04x, clk counter = %ld", raddr, rdata_tmp, *rdata, clk_cnt);
}

extern "C" void sdram_write(int32_t waddr, uint16_t wdata, int32_t wlen) {
    host_write(sdram + waddr, wlen, wdata);
    extern uint64_t clk_cnt;
    Log("sdram_write: waddr = 0x%08x, wdata = 0x%04x, wlen = %d, clk counter = %ld", waddr, wdata, wlen, clk_cnt);
}
