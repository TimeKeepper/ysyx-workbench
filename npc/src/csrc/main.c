#include "utils.h"
#include <main.h>

int main(int argc, char **argv) {
  #ifdef MARCO_TEST
  Log("This is a marco test");
  #endif
  init_monitor(argc, argv);

  engine_start(argc, argv);

  return is_exit_status_bad();
}
