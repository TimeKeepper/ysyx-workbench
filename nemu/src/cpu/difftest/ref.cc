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

#include "debug.h"
#include <cassert>
#include <cstdint>
#include <iostream>
#include <list>
#include <utility>
#include <vector>
#include <memory/icache.hpp>

extern "C"{

#include <isa.h>
#include <cpu/cpu.h>
#include <difftest-def.h>
#include <memory/paddr.h>
#include <string.h>

//NEMU作为REF

// 在DUT host memory的`buf`和REF guest memory的`addr`之间拷贝`n`字节,
// `direction`指定拷贝的方向, `DIFFTEST_TO_DUT`表示往DUT拷贝, `DIFFTEST_TO_REF`表示往REF拷贝

__EXPORT void difftest_memcpy(paddr_t addr, void *buf, size_t n, bool direction) {
  if(direction == DIFFTEST_TO_REF) {
    // memcpy(Guest_2_host_CODE(addr), buf, n);
    memcpy(guest_to_host(addr), buf, n);
  } else {
    memcpy(buf, guest_to_host(addr), n);
  }
}

// `direction`为`DIFFTEST_TO_DUT`时, 获取REF的寄存器状态到`dut`;
// `direction`为`DIFFTEST_TO_REF`时, 设置REF的寄存器状态为`dut`;
__EXPORT void difftest_regcpy(void *dut, bool direction) {
  if(direction == DIFFTEST_TO_DUT) {
    memcpy(dut, &cpu, sizeof(CPU_state));
  } else {
    memcpy(&cpu, dut, sizeof(CPU_state));
  }
}

__EXPORT void difftest_cache_init(paddr_t begin, paddr_t end, uint32_t way, uint32_t set, uint32_t block_size) {
  void Icache_init(paddr_t begin, paddr_t end, uint32_t way, uint32_t set, uint32_t block_size);
  Icache_init(begin, end, way, set, block_size);
}

Icache_return icache_fetch(vaddr_t addr, uint32_t len);
__EXPORT void difftest_cache_state(void *dut, bool direction) {
  auto src = reinterpret_cast<std::vector<std::vector<CacheLine>>*>(dut);
  if(direction == DIFFTEST_TO_DUT) {
    *src = icache.cache;
  } else {
    // icache.cache = *src;
    icache_fetch(cpu.pc, 4); // perhaps...
  }
}

__EXPORT void difftest_cache_print(void) {
  icache.print_cache();
}

__EXPORT void difftest_cache_behaior(void *dut) {
  memcpy(dut, &icache_behavior, sizeof(Icache_return));
}

// 让REF执行`n`条指令
__EXPORT void difftest_exec(uint64_t n) {
  Pin;
  cpu_exec(n);
}

__EXPORT void difftest_raise_intr(word_t NO) {
  assert(0);
}

__EXPORT void difftest_init(int port) {
  void init_mem();
  init_mem();
  
  /* Perform ISA dependent initialization. */
  init_isa();
}

}
