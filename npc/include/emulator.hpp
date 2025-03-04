#ifndef __EMULATOR_HPP__
#define __EMULATOR_HPP__

#include "cpu.hpp"
#include <cstdint>
#include <memory.hpp>
#include <memory>
#include <queue>
#include <utils.hpp>
#include <unordered_map>
#include <performence.hpp>

#include "verilated.h"

#ifdef CONFIG_waveForm_vcd
#include "verilated_vcd_c.h"
#endif
#ifdef CONFIG_waveForm_fst
#include "verilated_fst_c.h"
#endif

class Emulator {
    public:
        int argc;
        char **argv;

        uint64_t seed = 0;

        std::queue<std::pair<bool, bool>> icache_msg_transmiter;

        char* diff_so_file = NULL;
        char* elf_file = NULL;
        char* img_file = NULL;
        uint64_t img_size = 0;

        uint64_t run_inst_num = 0;

        const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
        TOP_NAME* top = new TOP_NAME;
        #ifdef CONFIG_waveForm_vcd
        VerilatedVcdC* tfp = new VerilatedVcdC;
        #endif
        #ifdef CONFIG_waveForm_fst
        VerilatedFstC* tfp = new VerilatedFstC;
        #endif
        bool wave_trace_on = false;
        void wave_trace_once();

        bool instruciton_trace_on = false;
        std::deque<std::pair<uint32_t, uint32_t>> instruction_buffer;
        uint32_t buffer_cap = 32;
        void instruction_buffer_push(uint32_t pc, uint32_t inst);

        std::string disasm(uint32_t pc, uint32_t inst);

        void parse_args();
        void init_rand();
        void init_mem();
        void init_isa();
        void load_image();
        void init_simulate();

        bool is_batch_mode = false;
        NPCState npc_state = { .state = NPC_STOP ,.halt_pc = 0, .halt_ret = 0};
        Riscv_CPU_State cpu;
        
        Emulator(int argc, char **argv);
        ~Emulator();

        std::unordered_map<std::string, std::unique_ptr<Memory>> memorys;
        
        std::unique_ptr<performence> perf;

        void reset(uint64_t n);
        void cycle(uint64_t n);
        const std::pair<const std::string, std::unique_ptr<Memory>>* find_match_memory(uint32_t addr);
        uint32_t memory_read(uint32_t addr);
        void single_inst(uint64_t n);
        void wave_trace_ctrl(bool v);
        void instruction_trace_ctrl(bool v);
        void instruction_buffer_print();

        void Emulator_trap(uint32_t a0);

        void IFU_catch(uint32_t inst);
        std::queue<uint32_t> Inst_quene;
        std::vector<std::vector<CacheLine>> cache;
        void Icache_catch(uint32_t map_hit, uint32_t cache_hit);
        void Icache_state_catch(uint32_t write_index, uint32_t write_way, uint32_t write_tag, const svBitVecVal* write_data);
        void Icache_flush();
        void Icache_MAT_catch(uint32_t count);
        void IDU_catch(performence::Inst_Type type);
        void ALU_catch();
        void LSU_catch();
        void WBU_catch(uint32_t next_pc, \
        uint32_t gpr_waddr, uint32_t gpr_wdata, \
        uint32_t csr_wen, uint32_t csr_waddr, uint32_t csr_wdata, \
        uint32_t csr_wenb, uint32_t csr_waddrb, uint32_t csr_wdatab);
};

#endif
