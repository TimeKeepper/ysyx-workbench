#include "emulator.hpp"
#include "nvboard.h"
#include <utils.hpp>
#include <Vtop.h>
#include <verilated_vcd_c.h>

void Emulator::wave_trace_once(){
    this->contextp->timeInc(1);
    this->tfp->dump(contextp->time());
}

Emulator::Emulator(int argc, char **argv){
    this->contextp->commandArgs(argc, argv);
    Verilated::traceEverOn(true);
    this->top->trace(tfp, 99);
    this->tfp->open("wave.vcd");

    void nvboard_bind_all_pins(Vtop* top);  
    nvboard_bind_all_pins(this->top);
    nvboard_init();
    Log("NVBoard " ANSI_FMT("ON", ANSI_FG_GREEN));

    this->reset(20);
}

Emulator::~Emulator(){
    this->tfp->close();
    nvboard_quit();
}

void Emulator::reset(uint64_t n) {
    this->top->reset = 0;
    cycle(n);
    this->top->reset = 1;
}

void Emulator::cycle(uint64_t n) {
    module_state.state = MODULE_RUNNING;
    for(;n > 0; n--) {
        this->top->clock = 0; top->eval();
        if(this->wave_trace_on) wave_trace_once();                  

        this->top->clock = 1; top->eval();
        if(this->wave_trace_on) wave_trace_once();  

        nvboard_update();

        if(module_state.state != MODULE_RUNNING) break;
    }
}

void Emulator::wave_trace_ctrl(bool v){
    std::cout << "Wave Trace " << (v ? ANSI_FG_GREEN : ANSI_FG_RED)
    << (v ? "ON" : "OFF") << ANSI_NONE << std::endl;
    this->wave_trace_on = v;
}
