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
static uint8_t *pmem = NULL;
#else // CONFIG_PMEM_GARRAY
static uint8_t mrom[MROM_SIZE] PG_ALIGN = {};
static uint8_t pmem[CONFIG_MSIZE] PG_ALIGN = {};
static uint8_t flash[FLASH_SIZE] PG_ALIGN = {};
#endif

#define CODE_MEMORY mrom
#define CODE_MEMORY_SIZE MROM_SIZE

uint8_t* guest_to_host_pmem(paddr_t paddr) { return pmem + paddr - CONFIG_MBASE; }
paddr_t host_to_guest_pmem(uint8_t *haddr) { return haddr - pmem + CONFIG_MBASE; }

uint8_t* guest_to_host_mrom(paddr_t paddr) { return mrom + paddr - MROM_BASE; }
paddr_t host_to_guest_mrom(uint8_t *haddr) { return haddr - mrom + MROM_SIZE; }

uint8_t* guest_to_host_flash(paddr_t paddr) { return flash + paddr - FLASH_BASE; }
paddr_t host_to_guest_flash(uint8_t *haddr) { return haddr - flash + FLASH_SIZE; }

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

void instr_buf_printf(void);
static void out_of_bound(paddr_t addr) {
  Log("address = " FMT_PADDR " is out of bound of pmem [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,
      addr, PMEM_LEFT, PMEM_RIGHT, cpu.pc);
  #ifndef CONFIG_DEVICE
  printf(ANSI_FMT("may be you should enable the function \"device\"\n", ANSI_FG_RED));
  #endif
  nemu_state.state = NEMU_ABORT;
}

void init_mem() {
#if   defined(CONFIG_PMEM_MALLOC)
  pmem = malloc(CONFIG_MSIZE);
  assert(pmem);
#endif
  IFDEF(CONFIG_MEM_RANDOM, memset(CODE_MEMORY, rand(), CODE_MEMORY_SIZE));
  Log("physical memory area [" FMT_PADDR ", " FMT_PADDR "]", PMEM_LEFT, PMEM_RIGHT);
}

word_t paddr_read(paddr_t addr, int len) {
  #ifdef CONFIG_MTRACE
  printf(ANSI_FMT("paddr read", ANSI_FG_BLUE) " addr = " FMT_PADDR ", len = %d\n", addr, len);
  #endif
  if (likely(in_pmem(addr))) return pmem_read(addr, len);
  else if(likely(in_mrom(addr))) return mrom_read(addr);
  IFDEF(CONFIG_DEVICE, return mmio_read(addr, len));
  out_of_bound(addr);
  return 0;
}

void paddr_write(paddr_t addr, int len, word_t data) {
  #ifdef CONFIG_MTRACE
  printf(ANSI_FMT("paddr write", ANSI_FG_BLUE) " addr = " FMT_PADDR ", len = %d, data = " FMT_WORD "\n", addr, len, data);
  #endif
  if (likely(in_pmem(addr))) { pmem_write(addr, len, data); return; }
  IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); return);
  out_of_bound(addr);
}
