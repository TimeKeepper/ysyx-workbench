#include <nvboard.h>
#include <Vtop.h>
#include "verilated.h"
#include "verilated_vcd_c.h"
#include <stdio.h>

#define WAVE_ON

static TOP_NAME dut;

void nvboard_bind_all_pins(Vtop* top);

static void single_cycle() {
  dut.clk = 0; dut.eval();
  dut.clk = 1; dut.eval();
}

static void reset(int n) {
  dut.rst = 1;
  while (n -- > 0) single_cycle();
  dut.rst = 0;
}

int main(int argc, char** argv) {
  #ifdef WAVE_ON
  const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};

	Verilated::traceEverOn(true);

  #endif
  
  nvboard_bind_all_pins(&dut);
  nvboard_init();

  #ifdef WAVE_ON
	contextp->commandArgs(argc, argv);

	VerilatedVcdC* tfp = new VerilatedVcdC;
	dut.trace(tfp, 99);
	tfp->open("wave.vcd");
  #endif

  reset(10);

  while(1) {
    nvboard_update();
    single_cycle();
    dut.eval();

    #ifdef WAVE_ON
    contextp->timeInc(1);
		tfp->dump(contextp->time());
		#endif
  }

  #ifdef WAVE_ON
	tfp->close();
	#endif
  nvboard_quit();

}
