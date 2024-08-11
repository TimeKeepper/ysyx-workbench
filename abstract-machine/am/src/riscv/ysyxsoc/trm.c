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

extern char _sheap, _eheap;
extern char _stext, data_load_start, data_load_size, _esdata;
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_sheap, &_eheap);

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

static uint8_t putch_counter = 0;

void putch(char ch) {
  if(putch_counter++ == 16){
    while(!(*((volatile uint8_t  *)SERIAL_LS) & 0x20)); //检查第5位是否为1，表示空闲
    putch_counter = 0;
  }
  outb(SERIAL_RB, ch);
}

void halt(int code) {
  asm volatile("mv a0, %0; ebreak" : :"r"(code));

  while(1);
}


int boot_loader(void) {
  uint32_t *dst = (uint32_t *)&_stext;
  const uint32_t *src = (uint32_t *)&data_load_start;
  size_t n = (size_t)(&data_load_size) / 4 + 1;// 确保全部加载
  while(n--){
    *dst++ = *src++;
  }

  // memcpy(&_sdata, &data_load_start, ((uintptr_t)(&_esdata) - (uintptr_t)(&_sdata)));
  uart_init();
  return 0;
}

#define READ_CSR(csr, var)                                   \
    do {                                                     \
        unsigned long __v;                                   \
        __asm__ __volatile__ ("csrr %0, " #csr ""      \
                             : "=r" (__v)                    \
                             :                               \
                             : "memory");                     \
        var = (typeof(var)) __v;                              \
    } while (0)

void _trm_init() {
  int ret;
  ret = boot_loader();
  uint32_t mvendorid;
  READ_CSR(mvendorid, mvendorid);
  printf("%d\n", mvendorid);
  ret = main(mainargs);
  halt(ret);
}
