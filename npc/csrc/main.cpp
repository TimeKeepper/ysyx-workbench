#include <nvboard.h>
#include <Vtop.h>
#include "verilated.h"
#include "verilated_vcd_c.h"
#include <stdio.h>

#define WAVE_ON

Vtop* top;

void nvboard_bind_all_pins(Vtop* top);

static void single_cycle() {
  top->clk = 0; top->eval();
  top->clk = 1; top->eval();
}

static void reset(int n) {
  top->rst = 1;
  while (n -- > 0) single_cycle();
  top->rst = 0;
}

int main(int argc, char** argv) {
  #ifdef WAVE_ON
  const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};

	Verilated::traceEverOn(true);

  #endif

  top = new Vtop;
  
  nvboard_bind_all_pins(top);
  nvboard_init();

  #ifdef WAVE_ON
	contextp->commandArgs(argc, argv);

	VerilatedVcdC* tfp = new VerilatedVcdC;
	top->trace(tfp, 99);
	tfp->open("wave.vcd");
  #endif

  reset(10);

  while(1) {
    nvboard_update();
    single_cycle();
    top->eval();

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
