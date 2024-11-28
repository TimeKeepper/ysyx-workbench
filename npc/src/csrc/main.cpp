#include <memory>
#include <utils.hpp>
#include <main.hpp>
#include <simple_debugger.hpp>
#include <csignal>
#include <emulator.hpp>

std::unique_ptr<Emulator> emulator;

void SDL_handle(int SIGNAL){
  Log("state: %d", emulator->npc_state.state);
  if(SIGNAL == SIGINT && emulator->npc_state.state == NPC_RUNNING){
    std::cout << ANSI_FG_CYAN << "Ctrl+C detected, stopping Emulator..." << ANSI_NONE << std::endl;
    emulator->npc_state.state = NPC_STOP;
  }
}

int main(int argc, char **argv) {
  Log(ANSI_FMT("REBUILD", ANSI_FG_GREEN));

  emulator = std::make_unique<Emulator>(argc, argv);
  Log("state: %d", emulator->npc_state.state);

  signal(SIGINT, SDL_handle);

  std::unique_ptr<simple_debugger> sdb = std::make_unique<simple_debugger>(emulator.get());

  Log("state: %d", emulator->npc_state.state);
  sdb->main_loop();

  // init_monitor(argc, argv);

  // engine_start(argc, argv);

  // return is_exit_status_bad();
}
