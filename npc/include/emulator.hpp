#ifndef __EMULATOR_HPP__
#define __EMULATOR_HPP__

#include "cpu/cpu.hpp"
#include <memory.hpp>
#include <utils.hpp>
#include <vector>
#include <unordered_map>

class Emulator {
    private:
        int argc;
        char **argv;

        uint64_t seed = 0;

        bool is_batch_mode = false;

        char* diff_so_file = NULL;
        char* elf_file = NULL;
        char* img_file = NULL;

        NPCState npc_state = { .state = NPC_STOP ,.halt_pc = 0, .halt_ret = 0};
        Riscv_CPU_State cpu;

        std::unordered_map<std::string, std::unique_ptr<Memory>> memorys;
    public:
        Emulator(int argc, char **argv);
        ~Emulator();

        void Emulator_mainLoop();
};

#endif
