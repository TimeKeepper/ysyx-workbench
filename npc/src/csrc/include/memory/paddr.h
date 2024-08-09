#ifndef __MEMORY_PADDR_H__
#define __MEMORY_PADDR_H__

#include <common.h>
#include <cstdint>
#include <stdint.h>
#include <stdio.h>

# define DEVICE_BASE    0xa0000000
#define SERIAL_PORT     (DEVICE_BASE + 0x00003f8)
#define RTC_ADDR        (DEVICE_BASE + 0x0000048)

#define PMEM_LEFT  ((paddr_t)DEFAULT_MSIZE)
#define PMEM_RIGHT ((paddr_t)DEFAULT_MBASE + DEFAULT_MSIZE - 1)
#define RESET_VECTOR (PMEM_LEFT)

uint8_t* guest_to_host_pmem(paddr_t paddr);
paddr_t host_to_guest_pmem(uint8_t *haddr);

uint8_t* guest_to_host_mrom(paddr_t paddr);
paddr_t host_to_guest_mrom(uint8_t *haddr);

static inline bool in_pmem(paddr_t addr) {
  return addr - DEFAULT_MBASE < DEFAULT_MSIZE;
}

static inline bool in_mrom(paddr_t addr) {
  return addr - MROM_BASE < MROM_SIZE;
}

static inline bool in_flash(paddr_t addr) {
  return addr - FLASH_BASE < FLASH_SIZE;
}

word_t paddr_read(paddr_t addr, int len);
void paddr_write(paddr_t addr, int len, word_t data);
uint8_t* get_pmem(void);

#endif