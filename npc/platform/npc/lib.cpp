#include "verilated.h"
#include "Vtop.h"
#include <cstdint>
#include <iostream>

extern "C" {
    // function define

    // runtime
    typedef void (*sram_read_fp)(int32_t addr, int32_t* data);
    typedef void (*sram_write_fp)(int32_t addr, int32_t data, int32_t strb);
    typedef void (*Uart_putc_fp)(int32_t ch); // is it necessary?
    
    // performence
    typedef void (*IFU_catch_fp)(uint32_t inst);
    typedef void (*Icache_catch_fp)(uint32_t map_hit, uint32_t cache_hit);
    typedef void (*IDU_catch_fp)(uint32_t type);
    typedef void (*ALU_catch_fp)();
    typedef void (*LSU_catch_fp)(uint32_t diff_skip);
    typedef void (*WBU_catch_fp)(uint32_t next_pc, \
        uint32_t gpr_waddr, uint32_t gpr_wdata, \
        uint32_t csr_wena, uint32_t csr_waddra, uint32_t csr_wdataa, \
        uint32_t csr_wenb, uint32_t csr_waddrb, uint32_t csr_wdatab); // suck

    static sram_read_fp rust_sram_read;
    static sram_write_fp rust_sram_write;

    static IFU_catch_fp rust_IFU_catch;
    static Icache_catch_fp rust_Icache_catch;
    static IDU_catch_fp rust_IDU_catch;
    static ALU_catch_fp rust_ALU_catch;
    static LSU_catch_fp rust_LSU_catch;
    static WBU_catch_fp rust_WBU_catch;

    void init_sram_read(sram_read_fp callback) {
        rust_sram_read = callback;
    }

    void init_sram_write(sram_write_fp callback) {
        rust_sram_write = callback;
    }

    void init_IFU_catch(IFU_catch_fp callback) {
        rust_IFU_catch = callback;
    }

    void init_Icache_catch(Icache_catch_fp callback) {
        rust_Icache_catch = callback;
    }

    void init_IDU_catch(IDU_catch_fp callback) {
        rust_IDU_catch = callback;
    }

    void init_ALU_catch(ALU_catch_fp callback) {
        rust_ALU_catch = callback;
    }

    void init_LSU_catch(LSU_catch_fp callback) {
        rust_LSU_catch = callback;
    }

    void init_WBU_catch(WBU_catch_fp callback) {
        rust_WBU_catch = callback;
    }

    // control api
    static Vtop* top;
    static VerilatedContext* context;

    void init(int argc, char **argv) {
        context = new VerilatedContext;
        context->commandArgs(argc, argv);
        top = new Vtop{context};
    }

    void tick() {
        top->eval();
    }

    void single_cycle() {
        top->clock = 0;
        tick();
        top->clock = 1;
        tick();
    }

    void reset(uint32_t cycle) {
        top->reset = 1;
        for (uint32_t i = 0; i < cycle; i++) {
            single_cycle();
        }
        top->reset = 0;
    }

    void quit() {
        top->final();
        delete top;
        delete context;
    }

    // dpic binding
    extern void sram_read(int32_t addr, int32_t* data) {
        rust_sram_read(addr, data);
    }

    extern void sram_write(int32_t addr, int32_t data, int32_t strb) {
        rust_sram_write(addr, data, strb);
    }

    extern void Uart_putc(int32_t ch){
        std::cout << (char)ch;
    }

    extern void IFU_catch(uint32_t inst) {
        rust_IFU_catch(inst);
    }

    extern void Icache_catch(uint32_t map_hit, uint32_t cache_hit) {
        rust_Icache_catch(map_hit, cache_hit);
    }

    extern void IDU_catch(uint32_t type) {
        rust_IDU_catch(type);
    }

    extern void ALU_catch() {
        rust_ALU_catch();
    }

    extern void LSU_catch(uint32_t diff_skip) {
        rust_LSU_catch(diff_skip);
    }

    extern void WBU_catch(uint32_t next_pc, \
        uint32_t gpr_waddr, uint32_t gpr_wdata, \
        uint32_t csr_wena, uint32_t csr_waddra, uint32_t csr_wdataa, \
        uint32_t csr_wenb, uint32_t csr_waddrb, uint32_t csr_wdatab) {
        rust_WBU_catch(next_pc, gpr_waddr, gpr_wdata, csr_wena, csr_waddra, csr_wdataa, csr_wenb, csr_waddrb, csr_wdatab);
    }

}
