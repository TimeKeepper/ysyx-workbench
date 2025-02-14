#ifndef __DIFFERTEST_HPP__
#define __DIFFERTEST_HPP__

#include "cpu.hpp"
#include "emulator.hpp"
#include <cstdint>
#include <memory.hpp>
#include <utils.hpp>
#include <vector>

enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };

typedef struct {
    word_t inst;
    bool map_hit;
    bool cache_hit;
} Icache_return;

class Differtest {
    private:
        bool is_skip_ref = false;
        void checkregs(Riscv_CPU_State *ref, vaddr_t pc);
        std::vector<uint32_t> mem_watch_points;
        void checkmems();
        void checkcache(Icache_return icache_state);

        Riscv_CPU_State *dut_r;
        NPCState* npc_state;
        std::function<void(int a0)> Emulator_trap;
        Emulator* emulator;
    public:
        void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction) = NULL;
        void (*ref_difftest_regcpy)(void *dut, bool direction) = NULL;
        void (*ref_difftest_cache_init)(paddr_t begin, paddr_t end, uint32_t way, uint32_t set, uint32_t block_size) = NULL;
        void (*ref_difftest_cache_state)(void* dut, bool direction) = NULL;
        void (*ref_difftest_cache_behaior)(void *dut) = NULL;
        void (*ref_difftest_exec)(uint64_t n) = NULL;
        void (*ref_difftest_raise_intr)(uint64_t NO) = NULL;

        Differtest(char *ref_so_file, long img_size, int port, Riscv_CPU_State* dut_r, \
            Memory *load_mem, NPCState* npc_state, \
            std::function<void(int a0)> emulator_trap_func, Emulator* emulator);
            
        bool isa_difftest_checkregs(Riscv_CPU_State *ref_r, vaddr_t pc);
        void add_mem_watch_point(uint32_t addr);
        void difftest_step(vaddr_t pc, Icache_return icache_state);
        void difftest_skip_ref();
};

#endif
