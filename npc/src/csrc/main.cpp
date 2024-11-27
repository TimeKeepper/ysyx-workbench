#include <memory>
#include <utils.hpp>
#include <main.hpp>
#include <simple_debugger.hpp>

#include <emulator.hpp>

int main(int argc, char **argv) {
  Log(ANSI_FMT("REBUILD", ANSI_FG_GREEN));

  std::unique_ptr<Emulator> emulator = std::make_unique<Emulator>(argc, argv);

  std::unique_ptr<simple_debugger> sdb = std::make_unique<simple_debugger>(emulator.get());

  sdb->main_loop(false);

  // init_monitor(argc, argv);

  // engine_start(argc, argv);

  // return is_exit_status_bad();
}
