#include <am.h>
#include <klib-macros.h>
#include <klib.h>
#include "../riscv.h"

// # define DEVICE_BASE 0xa0000000

#define SERIAL_PORT     (0x10000000)

extern char _heap_start;
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, &_heap_start + 0x00000fff);
#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

void putch(char ch) {
  outb(SERIAL_PORT, ch);
}

void halt(int code) {
  asm volatile("mv a0, %0; ebreak" : :"r"(code));

  while(1);
}

extern char _data, edata, data_load_start;
int boot_loader(void) {
  memcpy(&_data, &data_load_start, (&edata - &_data));
  return 0;
}

void _trm_init() {
  int ret;
  ret = boot_loader();
  ret = main(mainargs);
  halt(ret);
}
