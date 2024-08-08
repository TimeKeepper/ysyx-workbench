#include <am.h>
#include <klib-macros.h>
#include <klib.h>
#include "../riscv.h"

// # define DEVICE_BASE 0xa0000000

#define SERIAL_PORT     (0x10000000)
#define SERIAL_RB       (SERIAL_PORT + 0) // receive buffer
#define SERIAL_THR      (SERIAL_PORT + 0) // transmit holding register
#define SERIAL_IE       (SERIAL_PORT + 1) // interrupt enable
#define SERIAL_II       (SERIAL_PORT + 2) // interrupt identification
#define SERIAL_LCR      (SERIAL_PORT + 3) // line control register
#define SERIAL_MC       (SERIAL_PORT + 4) // modem control
#define SERIAL_LS       (SERIAL_PORT + 5) // line status
#define SERIAL_MS       (SERIAL_PORT + 6) // modem status

#define SERIAL_DLL      (SERIAL_PORT + 0) // divisor latch low
#define SERIAL_DLM      (SERIAL_PORT + 1) // divisor latch high

extern char _heap_start;
extern char _data, edata, data_load_start, data_size;
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, &_heap_start + 0x00000fff);

// Area data = RANGE(&_data, &_data + (int)&data_size);
#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

void uart_init(void){
  *((volatile uint8_t  *)SERIAL_LCR) = (*((volatile uint8_t  *)SERIAL_LCR)) | 0x80;

  *((volatile uint8_t  *)SERIAL_DLM) = 0x00;
  *((volatile uint8_t  *)SERIAL_DLL) = 0x01;
  
  *((volatile uint8_t  *)SERIAL_LCR) = (*((volatile uint8_t  *)SERIAL_LCR)) & 0x7f;
}

void putch(char ch) {
  outb(SERIAL_RB, ch);
}

void halt(int code) {
  asm volatile("mv a0, %0; ebreak" : :"r"(code));

  while(1);
}


int boot_loader(void) {
  memcpy(&_data, &data_load_start, (uint32_t)((uintptr_t)(&heap.start) - (uintptr_t)(&_data)));
  // memcpy(&_data, &data_load_start, (uint32_t)&data_size);
  uart_init();
  return 0;
}

void _trm_init() {
  int ret;
  ret = boot_loader();
  ret = main(mainargs);
  halt(ret);
}
