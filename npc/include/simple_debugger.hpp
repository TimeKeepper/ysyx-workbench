#ifndef __SIMPLE_DEBUGGER_HPP__
#define __SIMPLE_DEBUGGER_HPP__

#include <utils.hpp>
#include <functional>
#include <string>
#include <vector>
#include <emulator.hpp>
#include <expr.hpp>
#include <watch_point.hpp>
#include <differtest.hpp>

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

        std::unique_ptr<Expr> expr;
        std::unique_ptr<Watch_Point_Manager> wpm;
        bool is_watch_point_mode = false;
        void watch_point_mode(bool v);

        std::unique_ptr<Differtest> difftest;
    public:
        simple_debugger(Emulator* emulator);
        void main_loop();

        void LSU_catch(uint32_t diff_skip);
        void WBU_catch(void);
};

#endif
