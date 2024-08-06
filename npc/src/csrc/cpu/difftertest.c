// #include "VysyxSoCFull__Dpi.h"
#include "cpu/cpu.h"
#include "memory/paddr.h"
#include <assert.h>
#include <common.h>
#include <dlfcn.h>
#include <utils.h>
#include <sdb/sdb.h>

enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };

void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction) = NULL;
void (*ref_difftest_regcpy)(void *dut, bool direction) = NULL;
void (*ref_difftest_exec)(uint64_t n) = NULL;
void (*ref_difftest_raise_intr)(uint64_t NO) = NULL;

static bool is_skip_ref = false;
static int skip_dut_nr_inst = 0;

// this is used to let ref skip instructions which
// can not produce consistent behavior with NEMU
void difftest_skip_ref() {
  is_skip_ref = true;
  // If such an instruction is one of the instruction packing in QEMU
  // (see below), we end the process of catching up with QEMU's pc to
  // keep the consistent behavior in our best.
  // Note that this is still not perfect: if the packed instructions
  // already write some memory, and the incoming instruction in NEMU
  // will load that memory, we will encounter false negative. But such
  // situation is infrequent.
  skip_dut_nr_inst = 0;
}

// this is used to deal with instruction packing in QEMU.
// Sometimes letting QEMU step once will execute multiple instructions.
// We should skip checking until NEMU's pc catches up with QEMU's pc.
// The semantic is
//   Let REF run `nr_ref` instructions first.
//   We expect that DUT will catch up with REF within `nr_dut` instructions.
void difftest_skip_dut(int nr_ref, int nr_dut) {
  skip_dut_nr_inst += nr_dut;

  while (nr_ref -- > 0) {
    ref_difftest_exec(1);
  }
}

void init_difftest(char *ref_so_file, long img_size, int port) {
  #ifdef CONFIG_DIFFTEST
    assert(ref_so_file != NULL);

    void *handle;
    handle = dlopen(ref_so_file, RTLD_LAZY);
    assert(handle);

    ref_difftest_memcpy = (void (*)(paddr_t, void *, size_t, bool))dlsym(handle, "difftest_memcpy");
    assert(ref_difftest_memcpy);

    ref_difftest_regcpy = (void (*)(void *, bool))dlsym(handle, "difftest_regcpy");
    assert(ref_difftest_regcpy);

    ref_difftest_exec = (void (*)(uint64_t))dlsym(handle, "difftest_exec");
    assert(ref_difftest_exec);

    ref_difftest_raise_intr = (void (*)(uint64_t))dlsym(handle, "difftest_raise_intr");
    assert(ref_difftest_raise_intr);

    void (*ref_difftest_init)(int) = (void (*)(int))dlsym(handle, "difftest_init");
    assert(ref_difftest_init);

    printf("Differential testing: %s\n", ANSI_FMT("ON", ANSI_FG_GREEN));
    printf("The result of every instruction will be compared with %s. "
        "This will help you a lot for debugging, but also significantly reduce the performance. "
        "If it is not necessary, you can turn it off in menuconfig.\n", ref_so_file);

    ref_difftest_init(port);
    ref_difftest_memcpy(DEFAULT_MBASE, guest_to_host(DEFAULT_MBASE), img_size, DIFFTEST_TO_REF);
    ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);
  #endif
}

bool isa_difftest_checkregs(CPU_State *ref_r, vaddr_t pc) {
  if(ref_r->pc != cpu.pc){
    printf(ANSI_FG_RED "diffter test has detect an error!\n" ANSI_NONE);
    printf("reg:" ANSI_FG_YELLOW "%s" ANSI_NONE ", ref_value:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE ", dut_value:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE "\n", "pc", ref_r->pc, cpu.pc);
    return false;
  }
  for(int i = 0; i < 32; i++){
    if(ref_r->gpr[i] != cpu.gpr[i]){
        printf(ANSI_FG_RED "diffter test has detect an error!\n" ANSI_NONE);
        printf("reg:" ANSI_FG_YELLOW "%s" ANSI_NONE ", ref_value:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE ", dut_value:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE "\n", reg_id2name(i), ref_r->gpr[i], cpu.gpr[i]);
      return false;
    }
  }
  for(int i = 0; i < 5; i++){
    if(ref_r->sr[sregs_iddr[i]] != cpu.sr[sregs_iddr[i]]){
      printf(ANSI_FG_RED "diffter test has detect an error!\n" ANSI_NONE);
      printf("reg:" ANSI_FG_YELLOW "%d" ANSI_NONE ", ref_value:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE ", dut_value:" ANSI_FG_YELLOW "0x%08x" ANSI_NONE "\n", sregs_iddr[i], ref_r->sr[sregs_iddr[i]], cpu.sr[sregs_iddr[i]]);
      return false;
    }
  }
  return true;
}

#ifdef CONFIG_DIFFTEST
static void checkregs(CPU_State *ref, vaddr_t pc) {
  if (!isa_difftest_checkregs(ref, pc)) {
    npc_trap(1);
    npc_state.state = NPC_ABORT;
    npc_state.halt_pc = pc;
  }
}
#endif

void difftest_step(vaddr_t pc, vaddr_t npc) {
  #ifdef CONFIG_DIFFTEST
  CPU_State ref_r;

  if (skip_dut_nr_inst > 0) {
    ref_difftest_regcpy(&ref_r, DIFFTEST_TO_DUT);
    if (ref_r.pc == npc) {
      skip_dut_nr_inst = 0;
      checkregs(&ref_r, npc);
      return;
    }
    skip_dut_nr_inst --;
    if (skip_dut_nr_inst == 0){
      printf("can not catch up with ref.pc = 0x%08x at pc = 0x%08x", ref_r.pc, pc);
      assert(0);
    }
    return;
  }

  if (is_skip_ref) {
    // to skip the checking of an instruction, just copy the reg state to reference design
    ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);
    is_skip_ref = false;
    return;
  }

  ref_difftest_exec(1);
  ref_difftest_regcpy(&ref_r, DIFFTEST_TO_DUT);

  checkregs(&ref_r, pc);
  #endif
}