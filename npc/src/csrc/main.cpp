#include <utils.hpp>
#include <main.hpp>
#include <simple_debugger.hpp>

#include <emulator.hpp>

int main(int argc, char **argv) {
  Log(ANSI_FMT("REBUILD", ANSI_FG_GREEN));

  Emulator* emulator = new Emulator(argc, argv);

  std::unique_ptr<simple_debugger> sdb = std::make_unique<simple_debugger>(emulator);

  delete emulator;

  // init_monitor(argc, argv);

  // engine_start(argc, argv);

  // return is_exit_status_bad();
}
