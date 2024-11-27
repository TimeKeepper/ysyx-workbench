#ifndef __SIMPLE_DEBUGGER_HPP__
#define __SIMPLE_DEBUGGER_HPP__

#include <functional>
#include <string>
#include <utils.hpp>
#include <vector>

struct cmd {
    std::string name;
    std::string description;
    std::string usage;
    std::function<int(std::vector<std::string>)> func;
};

class simple_debugger {
    private:
        std::vector<cmd> cmds;
        NPCState* npc_state;
        std::function<void(int a0)> Emulator_trap;
    public:
        simple_debugger(NPCState* npc_state, std::function<void(int a0)> emulator_trap_func);
        void main_loop(bool is_batch_mode);
};

#endif
