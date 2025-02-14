#include "cpu.hpp"
#include "utils.hpp"
#include <cassert>
#include <differtest.hpp>
#include <dlfcn.h>
#include <string>

Differtest::Differtest(char *ref_so_file, long img_size, int port, \
    Riscv_CPU_State* dut_r, Memory *load_mem, NPCState* npc_state, \
    std::function<void(int a0)> emulator_trap_func, Emulator* emulator) \
    : dut_r(dut_r), npc_state(npc_state), Emulator_trap(emulator_trap_func), emulator(emulator) {
    assert(ref_so_file != NULL);

    void *handle;
    handle = dlopen(ref_so_file, RTLD_LAZY);
    assert(handle);

    ref_difftest_memcpy = (void (*)(paddr_t, void *, size_t, bool))dlsym(handle, "difftest_memcpy");
    assert(ref_difftest_memcpy);

    ref_difftest_regcpy = (void (*)(void *, bool))dlsym(handle, "difftest_regcpy");
    assert(ref_difftest_regcpy);

    ref_difftest_cache_init = (void (*)(paddr_t, paddr_t, uint32_t, uint32_t, uint32_t))dlsym(handle, "difftest_cache_init");
    assert(ref_difftest_cache_init);

    ref_difftest_cache_state = (void (*)(void *, bool))dlsym(handle, "difftest_cache_state");
    assert(ref_difftest_cache_state);

    ref_difftest_cache_behaior = (void (*)(void *))dlsym(handle, "difftest_cache_behaior");
    assert(ref_difftest_cache_behaior);

    ref_difftest_exec = (void (*)(uint64_t))dlsym(handle, "difftest_exec");
    assert(ref_difftest_exec);

    ref_difftest_raise_intr = (void (*)(uint64_t))dlsym(handle, "difftest_raise_intr");
    assert(ref_difftest_raise_intr);

    void (*ref_difftest_init)(int) = (void (*)(int))dlsym(handle, "difftest_init");
    assert(ref_difftest_init);

    Log("Differential testing: " ANSI_FMT("ON", ANSI_FG_GREEN));

    ref_difftest_init(port);
    ref_difftest_memcpy(CONFIG_LOAD_MEMORY_BASE, load_mem->get_memory(), img_size, DIFFTEST_TO_REF);
    ref_difftest_regcpy(dut_r, DIFFTEST_TO_REF);

    #ifdef CONFIG_PLATFORM_YSYXSOC
    ref_difftest_cache_init(CONFIG_SDRAM_BASE, CONFIG_SDRAM_BASE + CONFIG_SDRAM_SIZE, CONFIG_ICache_Way, CONFIG_ICache_Set, CONFIG_ICache_Block_Size);
    #elif CONFIG_PLATFORM_NPC
    ref_difftest_cache_init(CONFIG_LOAD_MEMORY_BASE, CONFIG_LOAD_MEMORY_BASE + CONFIG_LOAD_MEMORY_SIZE, CONFIG_ICache_Way, CONFIG_ICache_Set, CONFIG_ICache_Block_Size);
    #endif
}

bool Differtest::isa_difftest_checkregs(Riscv_CPU_State *ref_r, vaddr_t pc) {
    if(ref_r->pc != dut_r->pc){
        printf(ANSI_FG_RED "diffter test has detect an error!\n" ANSI_NONE);
        printf("reg:" ANSI_FG_YELLOW "%s" ANSI_NONE ", ref_value:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE ", dut_value:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE "\n", "pc", ref_r->pc, dut_r->pc);
        return false;
    }
    for(int i = 0; i < 32; i++){
        if(ref_r->gpr[i] != dut_r->gpr[i]){
            printf(ANSI_FG_RED "diffter test has detect an error!\n" ANSI_NONE);
            printf("reg:" ANSI_FG_YELLOW "%s" ANSI_NONE ", ref_value:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE ", dut_value:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE "\n", gpr_id2name(i), ref_r->gpr[i], dut_r->gpr[i]);
            return false;
        }
    }
    for(int i = 0; i < 5; i++){
        if(ref_r->sr[sregs_iddr[i]] != dut_r->sr[sregs_iddr[i]]){
            printf(ANSI_FG_RED "diffter test has detect an error!\n" ANSI_NONE);
            printf("reg:" ANSI_FG_YELLOW "%s" ANSI_NONE ", ref_value:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE ", dut_value:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE "\n", csr_id2name(sregs_iddr[i]), ref_r->sr[sregs_iddr[i]], dut_r->sr[sregs_iddr[i]]);
            return false;
        }
    }
    return true;
}

void Differtest::add_mem_watch_point(uint32_t addr){
    this->mem_watch_points.push_back(addr);
}

void Differtest::checkregs(Riscv_CPU_State *ref, vaddr_t pc){
    if (!isa_difftest_checkregs(ref, pc)) {
        npc_state->state = NPC_ABORT;
        npc_state->halt_pc = pc;
    }
}

void Differtest::checkmems(){
    uint32_t ref_data;
    uint32_t dut_data;
    for(const auto& addr: this->mem_watch_points) {
        ref_difftest_memcpy(addr, &ref_data, 4, DIFFTEST_TO_DUT);
        dut_data = this->emulator->memory_read(addr);
        if(ref_data != dut_data){
            printf(ANSI_FG_RED "diffter test has detect an error!\n" ANSI_NONE);
            printf("mem:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE ", ref_value:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE ", dut_value:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE "\n", addr, ref_data, dut_data);
            npc_state->state = NPC_ABORT;
            npc_state->halt_pc = dut_r->pc;
        }
    }
}

bool operator==(const CacheLine& a, const CacheLine& b) {
    return a.tag == b.tag && a.inst == b.inst && a.valid == b.valid;
}

bool operator!=(const CacheLine& a, const CacheLine& b) {
    return !(a == b);
}

void Differtest::checkcache(Icache_return icache_state){
    Icache_return ref_icache_state;
    ref_difftest_cache_behaior(&ref_icache_state);
    if(ref_icache_state.map_hit != icache_state.map_hit || ref_icache_state.cache_hit != icache_state.cache_hit){
        printf(ANSI_FG_RED "diffter test has detect an error!\n" ANSI_NONE);
        printf("icache: ref_inst:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE ", dut_inst:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE "\n", ref_icache_state.inst, icache_state.inst);
        printf("icache: ref_map_hit:" ANSI_FG_YELLOW "%d" ANSI_NONE ", dut_map_hit:" ANSI_FG_YELLOW "%d" ANSI_NONE "\n", ref_icache_state.map_hit, icache_state.map_hit);
        printf("icache: ref_cache_hit:" ANSI_FG_YELLOW "%d" ANSI_NONE ", dut_cache_hit:" ANSI_FG_YELLOW "%d" ANSI_NONE "\n", ref_icache_state.cache_hit, icache_state.cache_hit);
        npc_state->state = NPC_ABORT;
        npc_state->halt_pc = dut_r->pc;
    }

    std::vector<std::vector<CacheLine>> ref_cache_state;
    ref_cache_state.resize(CONFIG_ICache_Set, std::vector<CacheLine>(CONFIG_ICache_Way));
    ref_difftest_cache_state(&ref_cache_state, DIFFTEST_TO_DUT);

    // for(uint32_t i = 0; i < ref_cache_state.size(); i++){
    //     for(uint32_t j = 0; j < ref_cache_state[i].size(); j++){
    //         if(ref_cache_state[i][j] != emulator->cache[i][j]){
    //             printf(ANSI_FG_RED "diffter test has detect an error!\n" ANSI_NONE);
    //             printf("icache: ref_cache[" ANSI_FG_YELLOW "%d" ANSI_NONE "][" ANSI_FG_YELLOW "%d" ANSI_NONE "]:\n", i, j);
    //             printf("valid: ref_valid:" ANSI_FG_YELLOW "%d" ANSI_NONE ", dut_valid:" ANSI_FG_YELLOW "%d" ANSI_NONE "\n", ref_cache_state[i][j].valid, emulator->cache[i][j].valid);
    //             printf("tag: ref_tag:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE ", dut_tag:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE "\n", ref_cache_state[i][j].tag, emulator->cache[i][j].tag);
    //             printf("inst: ref_inst:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE ", dut_inst:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE "\n", ref_cache_state[i][j].inst, emulator->cache[i][j].inst);
    //             npc_state->state = NPC_ABORT;
    //             npc_state->halt_pc = dut_r->pc;
    //         }
    //     }
    // }
}

void Differtest::difftest_step(vaddr_t pc, Icache_return icache_state){
    Riscv_CPU_State ref_r;

    if (is_skip_ref) {
        ref_difftest_regcpy(this->dut_r, DIFFTEST_TO_REF);

        // ref_difftest_cache_state(this->emulator->cache.data(), DIFFTEST_TO_REF);

        is_skip_ref = false;
        return;
    }

    ref_difftest_exec(1);
    ref_difftest_regcpy(&ref_r, DIFFTEST_TO_DUT);

    checkmems();
    checkregs(&ref_r, pc);
    checkcache(icache_state);
}

void Differtest::difftest_skip_ref() {
    is_skip_ref = true;
}
