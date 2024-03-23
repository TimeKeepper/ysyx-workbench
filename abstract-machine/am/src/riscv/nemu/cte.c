#include <am.h>
#include <riscv/riscv.h>
#include <klib.h>

static Context* (*user_handler)(Event, Context*) = NULL;

Context* __am_irq_handle(Context *c) {
  if (user_handler) {
    Event ev = {0};
    switch (c->mcause) {
      case 0x0000000b:  ev.event = EVENT_YIELD;  break;
      default:          ev.event = EVENT_ERROR;  break;
    }

    c = user_handler(ev, c);
    assert(c != NULL);
  }

  return c;
}

extern void __am_asm_trap(void);

bool cte_init(Context*(*handler)(Event, Context*)) {
  // initialize exception entry
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));

  // register event handler
  user_handler = handler;

  return true;
}

typedef void (*FunctionPtr)(void *);

void combine_func(Context* con){
  FunctionPtr func_ptr;
  func_ptr = (FunctionPtr)con->tentry;
  func_ptr(con->arg);
} 

Context *kcontext(Area kstack, void (*entry)(void *), void *arg) {
  Context* con = (kstack.end - sizeof(Context));
  con->mstatus = 0x1800;
  con->tentry = (void *)entry;
  con->mepc = (uintptr_t)combine_func;
  con->gpr[2] = (uintptr_t)con;
  con->arg = arg;
  return con;
}

void yield() {
#ifdef __riscv_e
  asm volatile("li a5, -1; ecall");
#else
  asm volatile("li a7, -1; ecall");
#endif
}

bool ienabled() {
  return false;
}

void iset(bool enable) {
}
