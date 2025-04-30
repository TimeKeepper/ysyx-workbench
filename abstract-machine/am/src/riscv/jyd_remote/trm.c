#include <am.h>
#include <klib-macros.h>
#include <klib.h>
#include "../riscv.h"

// # define DEVICE_BASE 0xa0000000

#define GPIO_BASE (0x10002000)
#define GPIO_LED  (GPIO_BASE + 0x00)
#define GPIO_KEY  (GPIO_BASE + 0x04)
#define GPIO_SEG  (GPIO_BASE + 0x08)

#define CLINT_BASE (0x2000000)

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

extern char _begin, _stext;
extern char _etext, _srodata;
extern char _erodata, _sdata;
extern char _edata;

extern char _srodata_load;

extern char _sdata_load;
extern char _sdata, _edata;

extern char _sbss_load;
extern char _sbss, _ebss;

extern char _stack_top, _stack_pointer;
extern char _sheap, _eheap;

int main(const char *args);

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

char getch(void){
  if((inb(SERIAL_LS) & 0x1) == 0) return 0xff;
  return inb(SERIAL_RB);
}

void halt(int code) {
  asm volatile("mv a0, %0; ebreak" : :"r"(code));

  while(1);
}

static void inline boot_memcpy(void *dst, const void *src, size_t n){ // 在bootloader运行的时候,memcpy还没有正常加载
  uint32_t *d = (uint32_t *)dst;
  const uint32_t *s = (uint32_t *)src;
  while(n--){
    *d++ = *s++;
  }
}

// int FSBL(void){
//   uint32_t *dst = (uint32_t *)&_sssbl;
//   const uint32_t *src = (uint32_t *)&_sssbl_load;
//   size_t n = (size_t)(&_essbl - &_sssbl) / 4;// 确保全部加载

//   boot_memcpy(dst, src, n); // 程序加载

//   return 0;
// }

// int SSBL(void) {
//   uint32_t *dst = (uint32_t *)&_stext;
//   const uint32_t *src = (uint32_t *)&_stext_load;
//   size_t n = (size_t)(&_etext - &_stext);// 确保全部加载

//   boot_memcpy(dst, src, n); // 程序加载

//   dst = (uint32_t *)&_srodata;
//   src = (uint32_t *)&_srodata_load;
//   n = (size_t)(&_erodata - &_srodata);

//   boot_memcpy(dst, src, n); 

//   dst = (uint32_t *)&_erodata;
//   src = (uint32_t *)((uint32_t)&_srodata_load + n);
//   n = (size_t)(&_sdata - &_erodata);

//   boot_memcpy(dst, src, n);

//   dst = (uint32_t *)&_sdata;
//   src = (uint32_t *)&_sdata_load;
//   n = (size_t)(&_edata - &_sdata);

//   boot_memcpy(dst, src, n); // 数据加载

//   dst = (uint32_t *)&_sbss;
//   src = (uint32_t *)&_sbss_load;
//   n = (size_t)(&_ebss - &_sbss);

//   boot_memcpy(dst, src, n); // bss加载

//   *(volatile uint32_t*)GPIO_SEG = 0x23a6a198;//在nvboard的数码管上显示学号
//   uint32_t time = *(volatile uint32_t*)CLINT_BASE; //进行一次读取,初始化clint
//   (void)time; //该数据并用不到

//   return 0;
// }

#define READ_CSR(csr, var)                                   \
    do {                                                     \
        unsigned long __v;                                   \
        __asm__ __volatile__ ("csrr %0, " #csr ""      \
                             : "=r" (__v)                    \
                             :                               \
                             : "memory");                     \
        var = (typeof(var)) __v;                              \
    } while (0)

void _print_creater_info(void){
  uint32_t mvendorid;
  READ_CSR(mvendorid, mvendorid);

  char ysyx[5];
  ysyx[0] = (mvendorid >> 24) & 0xff;
  ysyx[1] = (mvendorid >> 16) & 0xff;
  ysyx[2] = (mvendorid >> 8 ) & 0xff;
  ysyx[3] = (mvendorid >> 0 ) & 0xff;
  ysyx[4] = '\0';

  uint32_t creater_id;
  READ_CSR(marchid, creater_id);

  printf("This processor is created by %s_%d\n", ysyx, creater_id);
}

int BL(void){
  uint32_t *dst = (uint32_t *)&_sdata;
  const uint32_t *src = (uint32_t *)&_sdata_load;
  size_t n = (size_t)(&_edata - &_sdata) / 4;// 确保全部加载

  boot_memcpy(dst, src, n); // 程序加载

  return 0;
}

void _trm_init() {
  int ret;
  ret = BL();

  ret = main(mainargs);
  halt(ret);
}
