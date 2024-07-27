#include <main.h>

int main(int argc, char **argv) {
  Verilated::commandArgs(argc, argv);
  // init_monitor(argc, argv);

  // nvboard_bind_all_pins(&dut);
  // nvboard_init();

  // engine_start(argc, argv);
  
  // nvboard_quit();
  // int a = is_exit_status_bad();
  printf("exit\n");
  Verilated::gotFinish(true); // 指示仿真完成
  pthread_exit(NULL);
  exit(0);
}
