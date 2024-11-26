#include "utils.h"
#include <main.h>

int main(int argc, char **argv) {
  init_monitor(argc, argv);

  engine_start(argc, argv);

  return is_exit_status_bad();
}
