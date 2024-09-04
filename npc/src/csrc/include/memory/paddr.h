#ifndef __MEMORY_PADDR_H__
#define __MEMORY_PADDR_H__

#include <common.h>
#include <cstdint>
#include <stdint.h>
#include <stdio.h>

# define DEVICE_BASE    0xa0000000
#define SERIAL_PORT     (DEVICE_BASE + 0x00003f8)
#define RTC_ADDR        (DEVICE_BASE + 0x0000048)

#define RESET_VECTOR    CONFIG_LOAD_MEMORY_BASE

#define SRAM_LEFT  ((paddr_t)CONFIG_SRAM_BASE)
#define SRAM_RIGHT ((paddr_t)CONFIG_SRAM_BASE + CONFIG_SRAM_SIZE - 1)

#define MROM_LEFT  ((paddr_t)CONFIG_MROM_BASE)
#define MROM_RIGHT ((paddr_t)CONFIG_MROM_BASE + CONFIG_MROM_SIZE - 1)

#define FLASH_LEFT  ((paddr_t)CONFIG_FLASH_BASE)
#define FLASH_RIGHT ((paddr_t)CONFIG_FLASH_BASE + CONFIG_FLASH_SIZE - 1)

#define PSRAM_LEFT  ((paddr_t)CONFIG_PSRAM_BASE)
#define PSRAM_RIGHT ((paddr_t)CONFIG_PSRAM_BASE + CONFIG_PSRAM_SIZE - 1)

uint8_t* guest_to_host_psram(paddr_t paddr);
paddr_t host_to_guest_psram(uint8_t *haddr);

uint8_t* guest_to_host_mrom(paddr_t paddr);
paddr_t host_to_guest_mrom(uint8_t *haddr);

uint8_t* guest_to_host_flash(paddr_t paddr);
paddr_t host_to_guest_flash(uint8_t *haddr);

uint8_t* guest_to_host(paddr_t paddr);

static inline bool in_sram(paddr_t addr) {
  return addr - CONFIG_SRAM_BASE < CONFIG_SRAM_SIZE;
}

static inline bool in_psram(paddr_t addr) {
  return addr - CONFIG_PSRAM_BASE < CONFIG_PSRAM_SIZE;
}

static inline bool in_mrom(paddr_t addr) {
  return addr - CONFIG_MROM_BASE < CONFIG_MROM_SIZE;
}

static inline bool in_flash(paddr_t addr) {
  return addr - CONFIG_FLASH_BASE < CONFIG_FLASH_SIZE;
}

static inline bool in_pmem(paddr_t addr){
  return (in_sram(addr) || in_mrom(addr) || in_flash(addr) || in_psram(addr));
}

word_t paddr_read(paddr_t addr, int len);
void paddr_write(paddr_t addr, int len, word_t data);
uint8_t* get_pmem(void);
uint8_t* get_flash(void);

#endif