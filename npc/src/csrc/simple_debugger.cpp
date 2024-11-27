#include <simple_debugger.hpp>

#include <readline/readline.h>
#include <readline/history.h>
#include <algorithm>

simple_debugger::simple_debugger(NPCState* npc_state, std::function<void(int a0)> emulator_trap_func) : npc_state(npc_state), Emulator_trap(emulator_trap_func) {
    cmds.push_back(
        {"help", "Print this help message", "help", \
        [&](std::vector<std::string> args){
            for(auto c : cmds){
                std::cout << ANSI_FG_YELLOW << c.name.c_str() << ANSI_NONE << '\t' << \
                ": " << ANSI_FG_CYAN << c.description << ANSI_NONE << std::endl;
            }
            return 0;
        }});

    cmds.push_back(
        {"q", "Quit the debugger", "q", \
        [&](std::vector<std::string> args){
            this->npc_state->state = NPC_STOP;
            return -1;
        }});
}
static char* rl_gets() {
    static char *line_read = NULL;

    if (line_read) {
        free(line_read);
        line_read = NULL;
    }

    line_read = readline("(npc) ");

    if(history_length != 0){
        HIST_ENTRY *last_cmd = history_get(history_length);
        
        if(strcmp(last_cmd->line, line_read) == 0){
            return line_read;
        }

        if(strcmp(line_read, "") == 0){
            line_read = (char*)malloc(strlen(last_cmd->line) + 1);
            strcpy(line_read, last_cmd->line);
            return line_read;
        }
    }

    if (line_read && *line_read) {
        add_history(line_read); 
    }

    return line_read;
}

void simple_debugger::main_loop(bool is_batch_mode) {
    if (is_batch_mode) {
        TODO();
        return;
    }

    while (true) {
        std::string str = rl_gets();
        if (str.empty()) continue;

        std::string cmd = strtok((char*)str.c_str(), " ");
        if (cmd.empty()) continue;

        std::vector<std::string> args;
        for (char *p = strtok(NULL, " "); p; p = strtok(NULL, " ")) {
            args.push_back(p);
        }

        auto it = std::find_if(cmds.begin(), cmds.end(), [&](const struct cmd& c) {
            return c.name == cmd;
        });

        if (it == cmds.end()) {
            printf("Unknown Command: %s\n", cmd.c_str());
            continue;
        }

        if (it->func(args) < 0) return;
    }
}
