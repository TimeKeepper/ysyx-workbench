#include <main.h>

static TOP_NAME dut;

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
int sim_stop (void){
  is_sim_complete = true;
  return clk_cnt;
}

int main(int argc, char **argv) {
  #ifdef TRACE
  const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
	Verilated::traceEverOn(true);
  #endif

  nvboard_bind_all_pins(&dut);
  nvboard_init();

  reset(10);
  inst_ram_init();

  #ifdef TRACE
	contextp->commandArgs(argc, argv);

	VerilatedVcdC* tfp = new VerilatedVcdC;
	top->trace(tfp, 99);
	tfp->open("wave.vcd");
  #endif

  while(!is_sim_complete) {
    clk_cnt++;

    nvboard_update();
    single_cycle();
    dut.inst = inst_ram_read((uint32_t)dut.pc);
    dut.eval();

    printf("r1: %d inst: %d\n", dut.test1, dut.inst);

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
