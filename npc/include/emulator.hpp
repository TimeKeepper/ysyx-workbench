#ifndef __EMULATOR_HPP__
#define __EMULATOR_HPP__

#include "cpu/cpu.hpp"
#include <memory.hpp>
#include <memory>
#include <utils.hpp>
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
        uint64_t img_size = 0;

        Riscv_CPU_State cpu;

        std::unordered_map<std::string, std::unique_ptr<Memory>> memorys;

        void parse_args();
        void init_rand();
        void init_mem();
        void init_isa();
        void load_image();
    public:
        NPCState npc_state = { .state = NPC_STOP ,.halt_pc = 0, .halt_ret = 0};
        Emulator(int argc, char **argv);
        ~Emulator();

        void Emulator_trap(uint32_t a0);
};

#endif
