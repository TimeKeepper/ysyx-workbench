#ifndef __EMULATOR_HPP__
#define __EMULATOR_HPP__

#include <utils.hpp>

class Emulator {
    private:
        NPCState npc_state = { .state = NPC_STOP ,.halt_pc = 0, .halt_ret = 0};
    public:
        Emulator();
        ~Emulator();

        void Emulator_mainLoop(int argc, char **argv);
};

#endif
