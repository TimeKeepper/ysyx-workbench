#include "nvboard.h"
#include <utils.hpp>
#include <Vtop.h>
#include <verilated_vcd_c.h>

const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
Vtop* top = new Vtop;
VerilatedVcdC* tfp = new VerilatedVcdC;

void Init_wavetrace(int argc, char **argv){
    contextp->commandArgs(argc, argv);
    Verilated::traceEverOn(true);
    top->trace(tfp, 99);
    tfp->open("wave.vcd");
    Log("Wave Trace " ANSI_FMT(" ON", ANSI_FG_GREEN));
}

static bool wave_trace_on = false;

void wave_Trace_on(){
    std::cout << "Wave Trace" << ANSI_FMT("ON", ANSI_FG_GREEN) << std::endl;
    wave_trace_on = true;
}

void wave_Trace_close(){
    tfp->close();
}

void wave_Trace_once(){
    if(!wave_trace_on) return;
    contextp->timeInc(1);
    tfp->dump(contextp->time());
}

static void single_cycle() {
    top->clock = 0; top->eval();wave_Trace_once();                  

    top->clock = 1; top->eval();wave_Trace_once();    

    nvboard_update();
}

void reset(int n) {
    top->reset = 0;
    while (n -- > 0) single_cycle();
    top->reset = 1;
}

void module_exec(uint64_t n) {
    module_state.state = MODULE_RUNNING;
    for(;n > 0; n--) {
        single_cycle();
        if(module_state.state == MODULE_STOP) break;
    }
}

void sdb_mainloop(void);
void engine_start(int argc, char **argv){
    reset(20);

    sdb_mainloop();
}
