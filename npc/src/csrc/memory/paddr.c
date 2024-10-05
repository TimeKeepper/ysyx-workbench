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

#ifdef PLATFORM_YSYXSOC

static uint8_t* psram;

static uint8_t* sdram;

static uint8_t* mrom;

static uint8_t* flash;

static uint8_t* vga;

uint8_t* guest_to_host_psram(paddr_t paddr) { return psram + paddr - CONFIG_PSRAM_BASE; }
paddr_t host_to_guest_psram(uint8_t *haddr) { return haddr - psram + CONFIG_PSRAM_BASE; }

uint8_t* guest_to_host_sdram(paddr_t paddr) { return sdram + paddr - CONFIG_SDRAM_BASE; }
paddr_t host_to_guest_sdram(uint8_t *haddr) { return haddr - sdram + CONFIG_SDRAM_SIZE; }

uint8_t* guest_to_host_mrom(paddr_t paddr) { return mrom + paddr - CONFIG_MROM_BASE; }
paddr_t host_to_guest_mrom(uint8_t *haddr) { return haddr - mrom + CONFIG_MROM_SIZE; }

uint8_t* guest_to_host_flash(paddr_t paddr) { return flash + paddr - CONFIG_FLASH_BASE; }
paddr_t host_to_guest_flash(uint8_t *haddr) { return haddr - flash + CONFIG_FLASH_SIZE; }

uint8_t* guest_to_host_vga(paddr_t paddr) { return vga + paddr - CONFIG_VGA_FRAME_BUFFER_BASE; }
paddr_t host_to_guest_vga(uint8_t *haddr) { return haddr - vga + CONFIG_VGA_FRAME_BUFFER_SIZE; }

#elif defined (PLATFORM_NPC)

static uint8_t* pmem;

uint8_t* guest_to_host_pmem(paddr_t paddr) { return pmem + paddr - CONFIG_LOAD_MEMORY_BASE; }

#endif

uint8_t* guest_to_host(paddr_t paddr) { 
    #ifdef PLATFORM_YSYXSOC
    if(in_psram(paddr)) return guest_to_host_psram(paddr);
    else if(in_sdram(paddr)) return guest_to_host_sdram(paddr);
    else if(in_mrom(paddr)) return guest_to_host_mrom(paddr);
    else if(in_flash(paddr)) return guest_to_host_flash(paddr);
    #elif defined (PLATFORM_NPC)
    if(in_pmem(paddr)) return guest_to_host_pmem(paddr);
    #endif
    else return nullptr;
}

#ifdef PLATFORM_YSYXSOC
#define LOAD_MEMORY flash
#elif defined (PLATFORM_NPC)
#define LOAD_MEMORY pmem
#endif

static const uint32_t img [] = {
  0x00000513,  // li a0 0
  0x00150513,  // add a0 a0 1
  0x00a12023,  // sw a0 0(sp)
  0xff9fffef,  // jmp 0x80000004
};

void mem_malloc(){
    #ifdef PLATFORM_YSYXSOC
    psram = (uint8_t*)malloc(CONFIG_PSRAM_SIZE);
    sdram = (uint8_t*)malloc(CONFIG_SDRAM_SIZE);
    mrom = (uint8_t*)malloc(CONFIG_MROM_SIZE);
    flash = (uint8_t*)malloc(CONFIG_FLASH_SIZE);
    vga = (uint8_t*)malloc(CONFIG_VGA_FRAME_BUFFER_SIZE);
    #elif defined (PLATFORM_NPC)
    pmem = (uint8_t*)malloc(CONFIG_LOAD_MEMORY_SIZE);
    #endif
}

void init_mem() {
    mem_malloc();
    memcpy(LOAD_MEMORY, img, sizeof(img));
    #ifdef PLATFORM_YSYXSOC
    Log("SRAM memory area \t [" "0x%08x" ", " "0x%08x" "]", SRAM_LEFT, SRAM_RIGHT);
    Log("MROM memory area \t [" "0x%08x" ", " "0x%08x" "]", MROM_LEFT, MROM_RIGHT);
    Log("VGA memory area \t [" "0x%08x" ", " "0x%08x" "]", VGA_LEFT, VGA_RIGHT);
    Log("FLASH memory area \t [" "0x%08x" ", " "0x%08x" "]", FLASH_LEFT, FLASH_RIGHT);
    Log("PSRAM memory area \t [" "0x%08x" ", " "0x%08x" "]", PSRAM_LEFT, PSRAM_RIGHT);
    Log("SDRAM memory area \t [" "0x%08x" ", " "0x%08x" "]", SDRAM_LEFT, SDRAM_RIGHT);
    #elif defined (PLATFORM_NPC)
    Log("PMEM memory area \t [" "0x%08x" ", " "0x%08x" "]", PMEM_LEFT, PMEM_RIGHT);
    #endif
}

#ifdef PLATFORM_YSYXSOC
extern "C" void flash_read(int32_t addr, int32_t *data) {
    int32_t data_tmp;
    uint32_t addr_tmp = (addr & ~0x3u);
    data_tmp = (host_read(flash + addr_tmp + 0, 1) << 24) | \
               (host_read(flash + addr_tmp + 1, 1) << 16) | \
               (host_read(flash + addr_tmp + 2, 1) << 8) | \
               (host_read(flash + addr_tmp + 3, 1) << 0);
    *data = data_tmp;
}

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
}

extern "C" void sdram_write(int32_t waddr, uint16_t wdata, int32_t wlen) {
    host_write(sdram + waddr, wlen, wdata);
    extern uint64_t clk_cnt;
}

extern "C" void clint_read(int32_t addr, int32_t *data) {
    if(addr == 0x02000000){
        *data = get_time();
    }else if(addr == 0x02000004){
        *data = get_time() >> 32;
    }
    return;
}

extern "C" void vga_read(int32_t x_addr, int32_t y_addr, uint32_t *rdata) {
    uint32_t raddr = (y_addr * 640 + x_addr) * 4;
    *rdata = host_read(vga + (raddr & ~0x3u), 4);
    // Log("vga_read: x_addr = 0x%08x, y_addr = 0x%08x, data = 0x%08x", x_addr, y_addr, *rdata);
}

extern "C" void vga_write(int32_t waddr, uint32_t wdata) {
    host_write(vga + waddr - CONFIG_VGA_FRAME_BUFFER_BASE, 4, wdata);
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
#elif defined (PLATFORM_NPC)

static word_t pmem_read(paddr_t addr, int len) {
    word_t ret = host_read(guest_to_host_pmem(addr & ~0x3u), len);
    return ret;
}

static void pmem_write(paddr_t addr, int len, word_t data) {
    host_write(guest_to_host_pmem(addr), len, data);
}

#endif

static void out_of_bound(paddr_t addr) {
    printf("address =  0x%08x  is out of bound", addr);
    npc_state.state = NPC_ABORT;
}

void difftest_skip_ref();

extern "C" word_t paddr_read(paddr_t addr, int len) {
    #ifdef PLATFORM_YSYXSOC
    if (likely(in_psram(addr))) return psram_read(addr, len);
    else if (likely(in_sdram(addr))) return sdram_read(addr, len);
    else if(likely(in_mrom(addr))) return mrom_read(addr);
    else if(likely(in_flash(addr))) return flash_read(addr);
    #elif defined (PLATFORM_NPC)
    if (in_pmem(addr)) return pmem_read(addr, len);
    #endif
    out_of_bound(addr);
    return 0;
}

extern "C" void paddr_write(paddr_t addr, int len, word_t data) {
    #ifdef PLATFORM_YSYXSOC
    if (likely(in_psram(addr))) {psram_write(addr, len, data); return; }
    else if(in_sdram(addr)) {sdram_write(addr, len, data); return; }
    #elif defined (PLATFORM_NPC)
    if (in_pmem(addr)) {pmem_write(addr, len, data); return; }
    #endif
    out_of_bound(addr);
}

extern "C" void paddr_write_strb(paddr_t addr, word_t data, int mask){
    switch(mask){
        case 0b0001: 
        case 0b0010: 
        case 0b0100: 
        case 0b1000: paddr_write(addr, 1, (data >> ((addr % 4) << 3)) & 0x000000ff); break;
        case 0b0011: 
        case 0b0110: 
        case 0b1100: paddr_write(addr, 2, (data >> ((addr % 4) << 3)) & 0x0000ffff); break;
        case 0b1111: paddr_write(addr, 4, data); break;
    }
}

uint8_t* get_loadmem(void) {
    #ifdef PLATFORM_YSYXSOC
    return flash;
    #elif defined (PLATFORM_NPC)
    return pmem;
    #endif
}
