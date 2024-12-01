#include <memory>
#include <utils.hpp>
#include <main.hpp>
#include <simple_debugger.hpp>
#include <csignal>
#include <emulator.hpp>

Emulator* emulator;

void SDL_handle(int SIGNAL){
  if(SIGNAL == SIGINT && emulator->npc_state.state == NPC_RUNNING){
    std::cout << ANSI_FG_CYAN << "Ctrl+C detected, stopping Emulator..." << ANSI_NONE << std::endl;
    emulator->npc_state.state = NPC_STOP;
  }
  std::cout << "(npc) ";
}

int main(int argc, char **argv) {
  Log(ANSI_FMT("REBUILD", ANSI_FG_GREEN));

  emulator = new Emulator(argc, argv);

  signal(SIGINT, SDL_handle);

  std::unique_ptr<simple_debugger> sdb = std::make_unique<simple_debugger>(emulator);

  sdb->main_loop();

  delete emulator;

  return 0;

  // init_monitor(argc, argv);

  // engine_start(argc, argv);

  // return is_exit_status_bad();
}
