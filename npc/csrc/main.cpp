#include <main.h>
#include <stdio.h>
#include <cpu/cpu.h>

// #define TRACE

int main(int argc, char **argv) {
  init_WaveTrace(argc, argv);

  init_monitor(argc, argv);

  // nvboard_bind_all_pins(&dut);
  // nvboard_init();

  cpu_reset(10, argc, argv); 

  while(1) {
    if(!cpu_exec(1)) break;
    trace_Once();
  }
  close_WaveTrace();
  // nvboard_quit();
}
