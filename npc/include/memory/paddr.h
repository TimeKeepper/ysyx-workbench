#ifndef __MEMORY_PADDR_H__
#define __MEMORY_PADDR_H__

#include <common.h>
#include <cstdint>
#include <stdio.h>

# define DEVICE_BASE 0xa0000000
#define SERIAL_PORT     (DEVICE_BASE + 0x00003f8)
#define RTC_ADDR        (DEVICE_BASE + 0x0000048)

#define PMEM_LEFT  ((paddr_t)DEFAULT_MSIZE)
#define PMEM_RIGHT ((paddr_t)DEFAULT_MBASE + DEFAULT_MSIZE - 1)
#define RESET_VECTOR (PMEM_LEFT)

/* convert the guest physical address in the guest program to host virtual address in NEMU */
uint8_t* guest_to_host(paddr_t paddr);
/* convert the host virtual address in NEMU to guest physical address in the guest program */
paddr_t host_to_guest(uint8_t *haddr);

static inline bool in_pmem(paddr_t addr) {
  return addr - DEFAULT_MBASE < DEFAULT_MSIZE || addr == SERIAL_PORT;
}

word_t paddr_read(paddr_t addr, int len);
void paddr_write(paddr_t addr, int len, word_t data);
uint8_t* get_pmem(void);

#endif