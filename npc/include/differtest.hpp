#ifndef __DIFFERTEST_HPP__
#define __DIFFERTEST_HPP__

#include "cpu.hpp"
#include "emulator.hpp"
#include <memory.hpp>
#include <utils.hpp>

enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };

class Differtest {
    private:
        bool is_skip_ref = false;
        void checkregs(Riscv_CPU_State *ref, vaddr_t pc);
        uint32_t checkmem_addr = 0x8001ac29;
        void checkmems();

        Riscv_CPU_State *dut_r;
        NPCState* npc_state;
        std::function<void(int a0)> Emulator_trap;
        Emulator* emulator;
    public:
        void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction) = NULL;
        void (*ref_difftest_regcpy)(void *dut, bool direction) = NULL;
        void (*ref_difftest_exec)(uint64_t n) = NULL;
        void (*ref_difftest_raise_intr)(uint64_t NO) = NULL;

        Differtest(char *ref_so_file, long img_size, int port, Riscv_CPU_State* dut_r, \
            Memory *load_mem, NPCState* npc_state, \
            std::function<void(int a0)> emulator_trap_func, Emulator* emulator);
            
        bool isa_difftest_checkregs(Riscv_CPU_State *ref_r, vaddr_t pc);
        void difftest_step(vaddr_t pc);
        void difftest_skip_ref();
};

#endif
