#include <utils.hpp>
#include <main.hpp>

#include <emulator.hpp>

int main(int argc, char **argv) {
  Log(ANSI_FMT("REBUILD", ANSI_FG_GREEN));

  Emulator* emulator = new Emulator();

  emulator->Emulator_mainLoop(argc, argv);

  delete emulator;

  // init_monitor(argc, argv);

  // engine_start(argc, argv);

  // return is_exit_status_bad();
}
