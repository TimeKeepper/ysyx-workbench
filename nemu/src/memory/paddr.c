/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <memory/host.h>
#include <memory/paddr.h>
#include <device/mmio.h>
#include <isa.h>
#include "common.h"
#include "utils.h"

#if   defined(CONFIG_PMEM_MALLOC)
static uint8_t *psram = NULL;
#else // CONFIG_PMEM_GARRAY
static uint8_t mrom [CONFIG_MROM_SIZE]  PG_ALIGN = {};
static uint8_t psram[CONFIG_PSRAM_SIZE] PG_ALIGN = {};
static uint8_t flash[CONFIG_FLASH_SIZE] PG_ALIGN = {};
static uint8_t sram [CONFIG_SRAM_SIZE]  PG_ALIGN = {};
static uint8_t sdram[CONFIG_SDRAM_SIZE] PG_ALIGN = {};
#endif

// #define CODE_MEMORY mrom
// #define CODE_MEMORY_SIZE MROM_SIZE

uint8_t* guest_to_host_sram(paddr_t paddr) { return sram + paddr - CONFIG_SRAM_BASE; }
paddr_t host_to_guest_sram(uint8_t *haddr) { return haddr - sram + CONFIG_SRAM_SIZE; }

uint8_t* guest_to_host_mrom(paddr_t paddr) { return mrom + paddr - CONFIG_MROM_BASE; }
paddr_t host_to_guest_mrom(uint8_t *haddr) { return haddr - mrom + CONFIG_MROM_SIZE; }

uint8_t* guest_to_host_flash(paddr_t paddr) { return flash + paddr - CONFIG_FLASH_BASE; }
paddr_t host_to_guest_flash(uint8_t *haddr) { return haddr - flash + CONFIG_FLASH_SIZE; }

uint8_t* guest_to_host_psram(paddr_t paddr) { return psram + paddr - CONFIG_PSRAM_BASE; }
paddr_t host_to_guest_psram(uint8_t *haddr) { return haddr - psram + CONFIG_PSRAM_BASE; }

uint8_t* guest_to_host_sdram(paddr_t paddr) { return sdram + paddr - CONFIG_SDRAM_BASE; }
paddr_t host_to_guest_sdram(uint8_t *haddr) { return haddr - sdram + CONFIG_SDRAM_BASE; }

uint8_t* guest_to_host(paddr_t paddr) {
  if (in_psram(paddr)) return guest_to_host_psram(paddr);
  else if(in_sram(paddr)) return guest_to_host_sram(paddr);
  else if(in_mrom(paddr)) return guest_to_host_mrom(paddr);
  else if(in_flash(paddr)) return guest_to_host_flash(paddr);
  else if(in_sdram(paddr)) return guest_to_host_sdram(paddr);
  return NULL;
}

static word_t sram_read(paddr_t addr, int len) {
    word_t ret = host_read(guest_to_host_sram(addr), len);
    return ret;
}

static void sram_write(paddr_t addr, int len, word_t data) {
  host_write(guest_to_host_sram(addr), len, data);
}

static word_t mrom_read(paddr_t addr, int len) {
    word_t ret = host_read(guest_to_host_mrom(addr), len);
    return ret;
}

static word_t flash_read(paddr_t addr, int len) {
    word_t ret = host_read(guest_to_host_flash(addr), len);
    return ret;
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

void instr_buf_printf(void);
static void out_of_bound(paddr_t addr) {
  Log("address = " FMT_PADDR " is out of bound of psram [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,
      addr, PMEM_LEFT, PMEM_RIGHT, cpu.pc);
  #ifndef CONFIG_DEVICE
  printf(ANSI_FMT("may be you should enable the function \"device\"\n", ANSI_FG_RED));
  #endif
  nemu_state.state = NEMU_ABORT;
}

void mem_random_set(void){
  memset(psram, rand(), CONFIG_PSRAM_SIZE);
  memset(flash, rand(), CONFIG_FLASH_SIZE);
  memset(sram,  rand(), CONFIG_SRAM_SIZE);
  memset(mrom,  rand(), CONFIG_MROM_SIZE);
  memset(sdram, rand(), CONFIG_SDRAM_SIZE);
}

void Icache_init(paddr_t begin, paddr_t end, uint32_t way, uint32_t set);

void init_mem() {
#if   defined(CONFIG_PMEM_MALLOC)
  psram = malloc(CONFIG_PSRAM_SIZE);
  assert(psram);
#endif
  IFDEF(CONFIG_MEM_RANDOM, mem_random_set());
  IFDEF(CONFIG_TARGET_SHARE, return;)
  Log("Config memory area \t [" FMT_PADDR ", " FMT_PADDR "]", PMEM_LEFT, PMEM_RIGHT);
  Log("SRAM memory area \t [" FMT_PADDR ", " FMT_PADDR "]", SRAM_LEFT, SRAM_RIGHT);
  Log("MROM memory area \t [" FMT_PADDR ", " FMT_PADDR "]", MROM_LEFT, MROM_RIGHT);
  Log("FLASH memory area \t [" FMT_PADDR ", " FMT_PADDR "]", FLASH_LEFT, FLASH_RIGHT);
  Log("PSRAM memory area \t [" FMT_PADDR ", " FMT_PADDR "]", PSRAM_LEFT, PSRAM_RIGHT);
  Log("SDRAM memory area \t [" FMT_PADDR ", " FMT_PADDR "]", SDRAM_LEFT, SDRAM_RIGHT);

  Icache_init(PSRAM_LEFT, PSRAM_RIGHT + 1, 8, 4);
}

word_t paddr_read(paddr_t addr, int len) {
  #ifdef CONFIG_MTRACE
  printf(ANSI_FMT("paddr read", ANSI_FG_BLUE) " addr = " FMT_PADDR ", len = %d\n", addr, len);
  #endif
  if (likely(in_psram(addr))) return psram_read(addr, len);
  else if(in_sram(addr)) return sram_read(addr, len);
  else if(in_mrom(addr)) return mrom_read(addr, len);
  else if(in_flash(addr)) return flash_read(addr, len);
  else if(in_sdram(addr)) return sdram_read(addr, len);
  IFDEF(CONFIG_DEVICE, return mmio_read(addr, len));
  out_of_bound(addr);
  return 0;
}

void paddr_write(paddr_t addr, int len, word_t data) {
  #ifdef CONFIG_MTRACE
  printf(ANSI_FMT("paddr write", ANSI_FG_BLUE) " addr = " FMT_PADDR ", len = %d, data = " FMT_WORD "\n", addr, len, data);
  #endif
  if (likely(in_psram(addr))) { psram_write(addr, len, data); return; }
  else if(in_sram(addr)) { sram_write(addr, len, data); return; }
  else if(in_sdram(addr)) { sdram_write(addr, len, data); return; }
  IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); return);
  out_of_bound(addr);
}
