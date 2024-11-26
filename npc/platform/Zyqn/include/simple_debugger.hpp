#ifndef __SIMPLE_DEBUGGER_HPP__
#define __SIMPLE_DEBUGGER_HPP__

#include <utils.hpp>
#include <readline/readline.h>
#include <readline/history.h>
#include <emulator.hpp>
#include <vector>

class simple_debugger {
    private:
    bool is_batch_mode = false;
    void set_batch_mode();

    Emulator& emulator;
    struct Command {
        const char *name;
        const char *description;
        const char *usage;
        int (*handler) (Emulator&,  char *);
    };
    std::vector<Command> cmd_table;
    int parse_args(int argc, char *argv[]);

    public:
    simple_debugger(Emulator& emulator, int argc, char **argv);
    ~simple_debugger();
    void sdb_mainloop(void);
};

#endif
