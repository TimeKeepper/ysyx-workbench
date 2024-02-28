#include <main.h>
#include <stdio.h>

static TOP_NAME dut;

static uint32_t inst_ram[RAM_SIZE];

#define TRACE

uint32_t inst_ram_read(uint32_t addr){
    addr %= RAM_SIZE;
    addr /= 4;
    return inst_ram[addr];
}

static void single_cycle() {
  dut.clk = 0; dut.eval();
  dut.clk = 1; dut.eval();
}

static void reset(int n) {
  dut.rst = 1;
  while (n -- > 0) single_cycle();
  dut.rst = 0;
}

bool is_sim_complete = false;

uint32_t clk_cnt = 0;
int sim_stop (int ra){
  is_sim_complete = true;
  if(ra == 1) printf("\033[1;32mHit good trap\033[0m\n");
  else printf("\033[1;31mHit bad trap\033[0m\n");
  return clk_cnt;
}

int main(int argc, char **argv) {
  #ifdef TRACE
	Verilated::traceEverOn(true);
  const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
	contextp->commandArgs(argc, argv);

	VerilatedVcdC* tfp = new VerilatedVcdC;
	dut.trace(tfp, 99);
	tfp->open("wave.vcd");
  #endif

  init_monitor(argc, argv, inst_ram);

  nvboard_bind_all_pins(&dut);
  nvboard_init();

  reset(10); 

  while(!is_sim_complete) {
    clk_cnt++;

    printf("pc: 0x%08x inst: %08x\n", dut.pc, dut.inst);

    nvboard_update();
    single_cycle();
    dut.inst = inst_ram_read((uint32_t)dut.pc);
    dut.eval();

    if(dut.inst == 0x00000000) {
      sim_stop(1);
    }

    #ifdef TRACE
    contextp->timeInc(1);
		tfp->dump(contextp->time());
		#endif
  }

  #ifdef TRACE
	tfp->close();
	#endif

  nvboard_quit();
}
