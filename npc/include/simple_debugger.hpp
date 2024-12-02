#ifndef __SIMPLE_DEBUGGER_HPP__
#define __SIMPLE_DEBUGGER_HPP__

#include <utils.hpp>
#include <functional>
#include <string>
#include <vector>
#include <emulator.hpp>

struct cmd {
    std::string name;
    std::string description;
    std::string usage;
    std::function<int(std::vector<std::string>)> func;
};

class simple_debugger {
    private:
        std::vector<cmd> cmds;
        Emulator* emulator;
    public:
        simple_debugger(Emulator* emulator);
        void main_loop();
};

#endif
