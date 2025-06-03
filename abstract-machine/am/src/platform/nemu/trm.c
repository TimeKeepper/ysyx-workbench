#include <am.h>
#include <nemu.h>

extern char _heap_start;
int main(const char *args);

Area heap = RANGE(&_heap_start, PMEM_END);
#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

extern int printf(const char *fmt, ...);

static Context* default_handler(Event ev, Context *c) {
  switch (c->mcause) {
    case 0x00000002: printf("IllegalInstruction"); halt(0); return c;
    default: printf("Unknown exception: %x", c->mcause); halt(0); return c;
  }
}

void putch(char ch) {
  outb(SERIAL_PORT, ch);
}

char getch(void){
  return 0;
}

void halt(int code) {
  nemu_trap(code);

  // should not reach here
  while (1);
}

void _trm_init() {
  cte_init(default_handler);
  int ret = main(mainargs);
  halt(ret);
}
