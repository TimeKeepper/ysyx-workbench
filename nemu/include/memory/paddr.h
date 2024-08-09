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

#ifndef __MEMORY_PADDR_H__
#define __MEMORY_PADDR_H__

#include <common.h>

#define CODE_MEMORY_BASE MROM_BASE
#define CODE_MEMORY_SIZE MROM_SIZE

#define PMEM_LEFT  ((paddr_t)CODE_MEMORY_BASE)
#define PMEM_RIGHT ((paddr_t)CODE_MEMORY_BASE + CODE_MEMORY_SIZE - 1)
#define RESET_VECTOR (PMEM_LEFT + CONFIG_PC_RESET_OFFSET)

uint8_t* guest_to_host_pmem(paddr_t paddr);
paddr_t host_to_guest_pmem(uint8_t *haddr);

uint8_t* guest_to_host_mrom(paddr_t paddr);
paddr_t host_to_guest_mrom(uint8_t *haddr);

static inline bool in_pmem(paddr_t addr) {
  return addr - CONFIG_MBASE < CONFIG_MSIZE;
}

static inline bool in_mrom(paddr_t addr) {
  return addr - MROM_BASE < MROM_SIZE;
}

word_t paddr_read(paddr_t addr, int len);
void paddr_write(paddr_t addr, int len, word_t data);

#endif
