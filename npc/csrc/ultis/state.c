#include <cstdio>
#include <utils.h>

NPCState npc_state = { .state = NPC_STOP ,.halt_pc = 0, .halt_ret = 0};

int is_exit_status_bad() {
  int good = (npc_state.state == NPC_END && npc_state.halt_ret == 0) ||
    (npc_state.state == NPC_QUIT);
  printf("npc_state.state: %d\n", npc_state.state);
  printf("npc_state.halt_ret: %d\n", npc_state.halt_ret);
  return !good;
}