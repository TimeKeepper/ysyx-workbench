#include <main.h>

int main(int argc, char **argv) {
  init_monitor(argc, argv);

  // nvboard_bind_all_pins(&dut);
  // nvboard_init();

  engine_start(argc, argv);
  
  // nvboard_quit();
  return is_exit_status_bad();
}
