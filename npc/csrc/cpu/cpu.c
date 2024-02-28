#include <cpu/cpu.h>
#include <memory/paddr.h>

static TOP_NAME dut;

CPU_State cpu = {};

const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
VerilatedVcdC* tfp = new VerilatedVcdC;

void init_WaveTrace(int argc, char **argv){
    #ifdef TRACE
    Verilated::traceEverOn(true);
    contextp->commandArgs(argc, argv);

    dut.trace(tfp, 99);
    tfp->open("wave.vcd");
    #endif
}

void trace_Once(){
    #ifdef TRACE
    contextp->timeInc(1);
    tfp->dump(contextp->time());
    #endif
}

void close_WaveTrace(){
    #ifdef TRACE
    tfp->close();
    #endif
}

uint32_t ram_read(uint32_t addr, int len){
    return paddr_read(addr, len);
}

void ram_write(paddr_t addr, int len, word_t data){
    paddr_write(addr, len, data);
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
    printf("ra: %d\n", ra);
    if(ra == 0) printf("\033[1;32mHit good trap\033[0m\n");
    else printf("\033[1;31mHit bad trap\033[0m\n");
    return clk_cnt;
    close_WaveTrace();
}

void cpu_reset(int n, int argc, char **argv){
    init_WaveTrace(argc, argv);
    reset(n); 
}

bool cpu_exec(uint64_t n){
    if(is_sim_complete) return false;

    clk_cnt++;

    printf("pc: 0x%08x inst: %08x\n", dut.pc, dut.inst);

    // nvboard_update();
    single_cycle();
    dut.inst = ram_read((uint32_t)dut.pc, 4);
    dut.eval();

    if(dut.inst == 0x00000000) {
      sim_stop(1);
    }

    cpu.pc = dut.pc;

    trace_Once();
    
    return true;
}
