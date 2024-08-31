#include <am.h>
#include <nemu.h>
#include <klib.h>

extern char _sheap, _eheap;
extern char _sssbl, _essbl, _sssbl_load;
extern char _stext, _etext, _stext_load;
extern char _srodata, _erodata, _srodata_load;
extern char _sdata_extra, _edata_extra, _sdata_extra_load;
extern char _sdata, _edata, _sdata_load;
extern char _sbss, _ebss, _sbss_load;
int main(const char *args);

Area heap = RANGE(&_sheap, &_eheap);
#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

void putch(char ch) {
  outb(SERIAL_PORT, ch);
}

void halt(int code) {
  nemu_trap(code);

  // should not reach here
  while (1);
}

static void inline boot_memcpy(void *dst, const void *src, size_t n){ // 在bootloader运行的时候,memcpy还没有正常加载
  while(n--){
    *(uint8_t *)dst++ = *(uint8_t *)src++;
  }
}

int FSBL(void){
  uint32_t *dst = (uint32_t *)&_sssbl;
  const uint32_t *src = (uint32_t *)&_sssbl_load;
  size_t n = (size_t)(&_essbl - &_sssbl);// 确保全部加载

  boot_memcpy(dst, src, n); // 程序加载

  return 0;
}

int SSBL(void) {
  uint32_t *dst = (uint32_t *)&_stext;
  const uint32_t *src = (uint32_t *)&_stext_load;
  size_t n = (size_t)(&_etext - &_stext);// 确保全部加载

  boot_memcpy(dst, src, n); // 程序加载

  dst = (uint32_t *)&_srodata;
  src = (uint32_t *)&_srodata_load;
  n = (size_t)(&_erodata - &_srodata);

  boot_memcpy(dst, src, n); 

  dst = (uint32_t *)&_erodata;
  src = (uint32_t *)((uint32_t)&_srodata_load + n);
  n = (size_t)(&_sdata - &_erodata);

  boot_memcpy(dst, src, n);

  dst = (uint32_t *)&_sdata;
  src = (uint32_t *)&_sdata_load;
  n = (size_t)(&_edata - &_sdata);

  boot_memcpy(dst, src, n); // 数据加载

  dst = (uint32_t *)&_sbss;
  src = (uint32_t *)&_sbss_load;
  n = (size_t)(&_ebss - &_sbss);

  boot_memcpy(dst, src, n); // bss加载

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

void _trm_init() {
  int ret;
  ret = FSBL();

  ret = SSBL();

  _print_creater_info();
  
  ret = main(mainargs);
  halt(ret);
}
