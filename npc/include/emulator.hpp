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

        char* diff_so_file = NULL;
        char* elf_file = NULL;
        char* img_file = NULL;
        uint64_t img_size = 0;


        uint64_t run_inst_num = 0;

        const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
        TOP_NAME* top = new TOP_NAME;
        VerilatedVcdC* tfp = new VerilatedVcdC;
        bool wave_trace_on = false;
        void wave_trace_once();

        bool instruciton_trace_on = false;
        std::deque<std::pair<uint32_t, uint32_t>> instruction_buffer;
        uint32_t buffer_cap = 32;
        void instruction_buffer_push(uint32_t pc, uint32_t inst);
        void instruction_buffer_print();

        std::string disasm(uint32_t pc, uint32_t inst);

        void parse_args();
        void init_rand();
        void init_mem();
        void init_isa();
        void load_image();
        void init_simulate();
    public:
        bool is_batch_mode = false;
        NPCState npc_state = { .state = NPC_STOP ,.halt_pc = 0, .halt_ret = 0};
        Riscv_CPU_State cpu;
        
        Emulator(int argc, char **argv);
        ~Emulator();

        std::unordered_map<std::string, std::unique_ptr<Memory>> memorys;
        
        void reset(uint64_t n);
        void cycle(uint64_t n);
        void single_inst(uint64_t n);
        void wave_trace_ctrl(bool v);
        void instruction_trace_ctrl(bool v);

        void Emulator_trap(uint32_t a0);

        void IFU_catch(uint32_t inst);
        void WBU_catch(uint32_t next_pc, \
        uint32_t gpr_waddr, uint32_t gpr_wdata, \
        uint32_t csr_wen, uint32_t csr_waddr, uint32_t csr_wdata);
};

#endif
