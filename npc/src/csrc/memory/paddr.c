#include "sdb/cmd.h"
#include "utils.h"
#include <cassert>
#include <common.h>
#include <cstdlib>
#include <cstring>
#include <memory/paddr.h>
#include <memory/host.h>
#include <stdio.h>

#define PG_ALIGN __attribute((aligned(4096)))

static uint8_t pmem[DEFAULT_MSIZE] PG_ALIGN = {};

static uint8_t mrom[MROM_SIZE] PG_ALIGN = {};

static uint8_t flash[FLASH_SIZE] PG_ALIGN = {};

uint8_t* guest_to_host_pmem(paddr_t paddr) { return pmem + paddr - DEFAULT_MBASE; }
paddr_t host_to_guest_pmem(uint8_t *haddr) { return haddr - pmem + DEFAULT_MBASE; }

uint8_t* guest_to_host_mrom(paddr_t paddr) { return mrom + paddr - MROM_BASE; }
paddr_t host_to_guest_mrom(uint8_t *haddr) { return haddr - mrom + MROM_SIZE; }

uint8_t* guest_to_host_flash(paddr_t paddr) { return flash + paddr - FLASH_BASE; }
paddr_t host_to_guest_flash(uint8_t *haddr) { return haddr - flash + FLASH_SIZE; }

#define CODE_MEMORY mrom

static const uint32_t img [] = {
  0x00000513,  // li a0 0
  0x00150513,  // add a0 a0 1
  0x00a12023,  // sw a0 0(sp)
  0xff9fffef,  // jmp 0x80000004
};

void init_mem() {
    for(uint32_t i = 0; i < FLASH_SIZE; i++){
        flash[i] = i & 0xff;
    }
    memcpy(CODE_MEMORY, img, sizeof(img));
}

static word_t pmem_read(paddr_t addr, int len) {
    word_t ret = host_read(guest_to_host_pmem(addr), len);
    return ret;
}

static void pmem_write(paddr_t addr, int len, word_t data) {
    host_write(guest_to_host_pmem(addr), len, data);
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
    printf("address =  0x%08x  is out of bound of pmem [ 0x%08x ,  0x%08x ]\n", 
    addr, PMEM_LEFT, PMEM_RIGHT);
    cmd_t(NULL);
    npc_state.state = NPC_ABORT;
}

void difftest_skip_ref();

word_t paddr_read(paddr_t addr, int len) {
    if (likely(in_pmem(addr))) return pmem_read(addr, len);
    else if(likely(in_mrom(addr))) return mrom_read(addr);
    else if(likely(in_flash(addr))) return flash_read(addr);
    else if(addr == RTC_ADDR ) {difftest_skip_ref(); return get_time();}
    else if(addr == RTC_ADDR + 4) { difftest_skip_ref(); return get_time() >> 32;}
    out_of_bound(addr);
    return 0;
}

void paddr_write(paddr_t addr, int len, word_t data) {
    if (likely(in_pmem(addr))) {pmem_write(addr, len, data); return; }
    out_of_bound(addr);
}

uint8_t* get_pmem(void) { //获取存放程序的内存节
    return CODE_MEMORY;
}

extern "C" void flash_read(int32_t addr, int32_t *data) { *data = host_read((flash + addr), 4); }
extern "C" void mrom_read(int32_t addr, int32_t *data) { *data = paddr_read(addr, 4); }
