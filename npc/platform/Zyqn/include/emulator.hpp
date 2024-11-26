#ifndef __EMULATOR_HPP__
#define __EMULATOR_HPP__

#include "nvboard.h"
#include <utils.hpp>
#include <Vtop.h>
#include <verilated_vcd_c.h>

class Emulator {
    private:
        const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
        Vtop* top = new Vtop;
        VerilatedVcdC* tfp = new VerilatedVcdC;
        bool wave_trace_on = false;
        void wave_trace_once();
    public:
        Emulator(int argc, char **argv);
        ~Emulator();
        void reset(uint64_t n);
        void cycle(uint64_t n);
        void wave_trace_ctrl(bool v);
};

#endif
