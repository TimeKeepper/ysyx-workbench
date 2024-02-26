#include <cstdint>
#include <stdio.h>
#include <nvboard.h>
#include <Vtop.h>
#include "verilated.h"
#include "verilated_vcd_c.h"
#include "Vtop__Dpi.h"

#define RAM_SIZE 1024

uint32_t inst_ram[RAM_SIZE];

void inst_ram_init(void) {
  for(int i = 0; i < RAM_SIZE; i++){
    inst_ram[i] = 0b00000000000100001000000010010011;
  }
  inst_ram[100] = 0x00100073;
}

uint32_t inst_ram_read(uint32_t addr){
    return inst_ram[addr];
}

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

bool is_sim_complete = false;

uint32_t clk_cnt = 0;
int sim_stop (void){
  is_sim_complete = true;
  printf("Simulation complete after %d cycles\n", clk_cnt);
  return clk_cnt;
}

int main() {
  nvboard_bind_all_pins(&dut);
  nvboard_init();

  reset(10);
  inst_ram_init();

  while(!is_sim_complete) {
    clk_cnt++;

    nvboard_update();
    single_cycle();
    dut.inst = inst_ram_read((uint32_t)dut.pc);
    dut.eval();

    printf("r1: %d inst: %d\n", dut.test1, dut.inst);
  }

  nvboard_quit();
}
