#include "differtest.hpp"
#include <iostream>
#include <numeric>
#include <simple_debugger.hpp>
#include <expr.hpp>
#include <readline/readline.h>
#include <readline/history.h>
#include <algorithm>
#include <iomanip>

std::string expr(std::string expr);

simple_debugger::simple_debugger(Emulator* emulator) : emulator(emulator) {
    cmds.push_back({
        "help", "Print this help message", "help", \
        [&](std::vector<std::string> args){
            for(auto c : cmds){
                std::cout << ANSI_FG_YELLOW << c.name.c_str() << ANSI_NONE << '\t' << \
                ": " << ANSI_FG_CYAN << c.description << ANSI_NONE << std::endl;
            }
            return 0;
        }
    });

    cmds.push_back({
        "q", "Quit the debugger", "q", \
        [&](std::vector<std::string> args){
            this->emulator->npc_state.state = NPC_STOP;
            return -1;
        }
    });

    cmds.push_back({
        "sc", "Step through N clock cycles", "sc N", \
        [&](std::vector<std::string> args){
            if(args.size() == 0){
                this->emulator->cycle(1);
                return 0;
            }

            int n = std::stoi(args[0]);
            if(n < 0){
                std::cout << ANSI_FG_RED << "You should input a positive value\n" << ANSI_NONE << std::endl;
                return 0;
            }
            else if(n == 0){
                std::cout << ANSI_FG_RED << "What do you mean, Bro?\n" << ANSI_NONE << std::endl;
                return 0;
            }

            this->emulator->cycle(n);
            return 0;
        }
    });

    cmds.push_back({
        "r", "Reset the emulator", "r", \
        [&](std::vector<std::string> args){
            this->emulator->reset(20);
            return 0;
        }
    });

    cmds.push_back({
        "c", "Continue the execution of the program", "c", \
        [&](std::vector<std::string> args){
            this->emulator->cycle(-1);
            return 0;
        }
    });

    cmds.push_back({
        "info", "Print information about the emulator", "info <r/w/b> <target>", \
        [&](std::vector<std::string> args){
            if(args.size() == 0){
                std::cout << ANSI_FG_RED << "You should input r/w/b" << ANSI_NONE << std::endl;
                return 0;
            }

            if(args[0] == "r"){
                if(args.size() == 1){
                    for(int i = 0; i < ARRLEN(this->emulator->cpu.gpr); i++){
                        std::cout << ANSI_FG_CYAN << gpr_id2name(i) \
                        << ANSI_NONE << "\t: " << ANSI_FG_BLUE \
                        << std::hex << "0x" << std::setw(8) << std::setfill('0') \
                        << this->emulator->cpu.gpr[i] << ANSI_NONE \
                        << std::endl;
                    }
                    return 0;
                }

                if(args[1] == "pc"){
                    std::cout << ANSI_FG_CYAN << "pc" << ANSI_NONE << "\t: " \
                    << ANSI_FG_BLUE << std::hex << "0x" << std::setw(8) << std::setfill('0') \
                    << this->emulator->cpu.pc << ANSI_NONE << std::endl;
                    return 0;
                }

                int target = gpr_name2id(args[1]);

                if (target != -1) {
                    std::cout << ANSI_FG_CYAN << args[1] << ANSI_NONE \
                    << "\t: " << ANSI_FG_BLUE \
                    << std::hex << "0x" << std::setw(8) << std::setfill('0') \
                    << this->emulator->cpu.gpr[target] \
                    << ANSI_NONE << std::endl;
                } else {
                    std::cout << ANSI_FG_RED << "Unknown register: " \
                    << args[1] << ANSI_NONE << std::endl;
                    return 0;
                }
            }else if(args[0] == "w"){
                this->wpm->print_watch_points();
            }else if(args[0] == "b"){
                this->wpm->print_break_points();
            }

            return 0;
        }
    });

    cmds.push_back({
        "x", "Examine memory", "x <len> <addr>", \
        [&](std::vector<std::string> args){
            if(args.size() != 2){
                std::cout << ANSI_FG_RED << "You should input two arguments" << ANSI_NONE << std::endl;
                return 0;
            }

            uint32_t addr = std::stoul(args[1], nullptr, 0);
            uint32_t len = std::stoul(args[0], nullptr, 0);

            auto match_memory = this->emulator->find_match_memory(addr);

            if(match_memory == nullptr) {
                std::cout << ANSI_FG_RED << "No memory found at address 0x" << std::hex << addr << ANSI_NONE << std::endl;
                return 0;
            }
            
            std::cout << ANSI_FG_CYAN << "Memory match on" << ANSI_NONE << "\t: " << ANSI_FG_BLUE << match_memory->first << ANSI_NONE << std::endl;

            for(int i = 0; i < len; i += 1){
                uint32_t data = match_memory->second->read_WithBias(addr + (i * 4), 4);
                std::cout << ANSI_FG_CYAN << "0x" << std::hex << addr + i << ANSI_NONE << "\t: " << ANSI_FG_BLUE << data << ANSI_NONE << std::endl;
            }

            return 0;
        }
    });

    cmds.push_back({
        "mm", "show memory map", "mm", \
        [&](std::vector<std::string> args){
            for(auto &m : this->emulator->memorys){
                std::cout << ANSI_FG_CYAN << m.first << ANSI_NONE << "\t: " \
                << '[' << ANSI_FG_BLUE << "0x" << std::hex \
                << m.second->base << ANSI_NONE\
                << ", " << ANSI_FG_BLUE << "0x" << std::hex \
                << m.second->base + m.second->size << ANSI_NONE << ']' \
                << std::endl;
            }
            return 0;
        }
    });

    cmds.push_back({
        "mdw", "add memory differtest watch point", \
        "mdw <addr>", \
        [&](std::vector<std::string> args){
            if(args.size() == 0){
                std::cout << ANSI_FG_RED << "You should input an address" << ANSI_NONE << std::endl;
                return 0;
            }

            uint32_t addr = std::stoul(args[0], nullptr, 0);
            this->difftest->add_mem_watch_point(addr);
            return 0;
        }
    });

    cmds.push_back({
        "si", "Step through one instruction", "si", \
        [&](std::vector<std::string> args){
            if(args.size() == 0){
                this->emulator->single_inst(1);
                return 0;
            }

            int n = std::stoi(args[0]);
            if(n < 0){
                std::cout << ANSI_FG_RED << "You should input a positive value\n" << ANSI_NONE << std::endl;
                return 0;
            }
            else if(n == 0){
                std::cout << ANSI_FG_RED << "What do you mean, Bro?\n" << ANSI_NONE << std::endl;
                return 0;
            }

            this->emulator->single_inst(n);
            return 0;
        }
    });

    cmds.push_back({
        "ir", "print instruction ring buffer", "ir", \
        [&](std::vector<std::string> args){
            this->emulator->instruction_buffer_print();
            return 0;
        }
    });

    cmds.push_back({
        "w", "Set a watchpoint", "w <expr>", \
        [&](std::vector<std::string> args){
            if(args.size() == 0){
                std::cout << ANSI_FG_RED << "You should input an expression" << ANSI_NONE << std::endl;
                return 0;
            }

            std::string expr = std::accumulate(args.begin(), args.end(), std::string(" "), [](std::string a, std::string b) {
                return a + b + " ";
            });
            if(this->wpm->add_watch_point(expr)){
                std::cout << ANSI_FG_CYAN << "Watchpoint set on" << ANSI_NONE << "\t: " << ANSI_FG_BLUE << expr << ANSI_NONE << std::endl;
            }else{
                std::cout << ANSI_FG_RED << "Invalid expression" << ANSI_NONE << std::endl;
            }

            return 0;
        }
    });

    cmds.push_back({
       "b", "Set a breakpoint", "b <addr>", \
       [&](std::vector<std::string> args){
           if(args.size() == 0){
               std::cout << ANSI_FG_RED << "You should input an address" << ANSI_NONE << std::endl;
               return 0;
           }

           std::string addr = args[0];
           if(this->wpm->add_break_point(addr)){
               std::cout << ANSI_FG_CYAN << "Breakpoint set on" << ANSI_NONE << "\t: " << ANSI_FG_BLUE << addr << ANSI_NONE << std::endl;
           }else{
               std::cout << ANSI_FG_RED << "Invalid expression" << ANSI_NONE << std::endl;
           }

           return 0;
       } 
    });

    cmds.push_back(
        {"test", "Test the expr", "test", \
        [&](std::vector<std::string> args){
            this->expr->test();
            return 0;
        }});

    cmds.push_back({
        "func", "Control Debug Function ON/OFF", "func <func> on/off", \
        [&](std::vector<std::string> args){
            if(args[0] == "help"){
                std::cout << ANSI_FG_BLUE << "avaliable functions: wave, inst" << ANSI_NONE << std::endl;
                return 0;
            }

            if(args.size() != 2){
                std::cout << ANSI_FG_RED << "You should input two arguments" << ANSI_NONE << std::endl;
                return 0;
            }

            std::unordered_map<std::string, std::function<void(bool)>> func_map = {
                {"wave", [&](bool on) { this->emulator->wave_trace_ctrl(on); }},
                {"inst", [&](bool on) { this->emulator->instruction_trace_ctrl(on); }}
            };

            if(args[1] != "on" && args[1] != "off"){
                std::cout << ANSI_FG_RED << "You should input on/off" << ANSI_NONE << std::endl;
                return 0;
            }

            auto it = func_map.find(args[0]);
            if (it != func_map.end()) {
                it->second(args[1] == "on");
            } else {
                std::cout << ANSI_FG_RED << "Unknown Function" << ANSI_NONE << std::endl;
            }

            return 0;
        }
    });

    cmds.push_back({
        "t", "Show performence count", "t", \
        [&](std::vector<std::string> args){
            this->emulator->perf->print_perf();
            return 0;
        }
    });

    this->expr = std::make_unique<Expr>();
    this->wpm = std::make_unique<Watch_Point_Manager>(this->expr.get());

    #ifdef CONFIG_DIFFTEST
    #ifdef CONFIG_PLATFORM_YSYXSOC
    this->difftest = std::make_unique<Differtest>(this->emulator->diff_so_file, this->emulator->img_size, 1234, &this->emulator->cpu, \
        this->emulator->memorys["flash"].get(), &this->emulator->npc_state, \
        [&](int a0) { this->emulator->Emulator_trap(a0); }, this->emulator);
    #elif defined (CONFIG_PLATFORM_NPC)
    this->difftest = std::make_unique<Differtest>(this->emulator->diff_so_file, this->emulator->img_size, 1234, &this->emulator->cpu, \
        this->emulator->memorys["sram"].get(), &this->emulator->npc_state, \
        [&](int a0) { this->emulator->Emulator_trap(a0); }, this->emulator);
    #endif
    #endif
}

void simple_debugger::watch_point_mode(bool v) {
    std::cout << "Watch Point Mode " << (v ? ANSI_FG_GREEN : ANSI_FG_RED) \
    << (v ? "ON" : "OFF") << ANSI_NONE << std::endl;
    this->is_watch_point_mode = v;
}

static char* rl_gets() {
    static char *line_read = NULL;

    if (line_read) {
        free(line_read);
        line_read = NULL;
    }

    line_read = readline(("(" + std::string(CONFIG_PLATFORM) + ") ").c_str());

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

void simple_debugger::main_loop() {
    if (this->emulator->is_batch_mode) {
        this->emulator->cycle(-1);
        return;
    }

    while (true) {
        std::string str = rl_gets();
        std::string str_bc = str;
        if (str.empty()) continue;

        std::string cmd = strtok((char*)str.c_str(), " ");

        std::vector<std::string> args;
        for (char *p = strtok(NULL, " "); p; p = strtok(NULL, " ")) {
            args.push_back(p);
        }

        auto it = std::find_if(cmds.begin(), cmds.end(), [&](const struct cmd& c) {
            return c.name == cmd;
        });

        if (it == cmds.end()) {
            // std::cout << ANSI_FG_BLUE << this->expr->eval(str_bc) << ANSI_NONE << std::endl;
            continue;
        }
        
        if (it->func(args) < 0) return;
    }
}

void simple_debugger::LSU_catch(uint32_t diff_skip){
    if(diff_skip == 0) return;
    
    #ifdef CONFIG_DIFFTEST
    this->difftest->difftest_skip_ref();
    #endif
}

void simple_debugger::WBU_catch(void) {
    #ifdef CONFIG_DIFFTEST
    auto msg = this->emulator->icache_msg_transmiter.front();
    this->emulator->icache_msg_transmiter.pop();

    this->difftest->difftest_step(this->emulator->cpu.pc, Icache_return{0, msg.first, msg.second});
    #endif

    if(this->wpm->check_watch_points() || this->wpm->check_break_points()){
        this->emulator->npc_state.state = NPC_STOP;
    }
}
