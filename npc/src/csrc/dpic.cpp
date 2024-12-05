#include <simple_debugger.hpp>
#include <memory>
#include <utils.hpp>
#include <emulator.hpp>

extern Emulator* emulator;
extern simple_debugger* sdb;

extern "C" {

    extern void flash_read(int32_t addr, int32_t* data) {
        *data = emulator->memorys["flash"]->read(addr, 4);
    }

    extern void mrom_read(int32_t addr, int32_t* data) {
        *data = emulator->memorys["mrom"]->read(addr, 4);
    }

    extern void psram_write(int32_t waddr, int32_t wdata, int32_t wlen){
        uint32_t wdata_tmp;
        switch(wlen){
            case 2: wdata_tmp = (wdata) >> 24;  break;
            case 4: wdata_tmp = (wdata) >> 16;  break;
            case 8: wdata_tmp = (wdata);        break;
            default: wdata_tmp = (wdata);       break;
        }
        emulator->memorys["psram"]->write(waddr, wlen, wdata_tmp);
    }

    extern void psram_read(int32_t addr, int32_t* data) {
        *data = emulator->memorys["psram"]->read(addr, 4);
    }

    extern void sdram_write(int32_t waddr, int32_t wdata, int32_t wlen) {
        emulator->memorys["sdram"]->write(waddr, wlen, wdata);
    }

    extern void sdram_read(int32_t addr, int32_t* data) {
        *data = emulator->memorys["sdram"]->read(addr, 2);
    }

    extern void vga_write(int32_t waddr, int32_t wdata) {
        emulator->memorys["vga"]->write(waddr - CONFIG_VGA_FRAME_BUFFER_BASE, 4, wdata);
    }

    extern void vga_read(int32_t x_addr, int32_t y_addr, int32_t* rdata) {
        uint32_t raddr = (y_addr * 640 + x_addr) * 4;
        *rdata = emulator->memorys["vga"]->read((raddr & ~0x3u), 4);
    }

    extern void IFU_catch(uint32_t inst){
        emulator->IFU_catch(inst);
    }

    extern void LSU_catch(uint32_t diff_skip){
        emulator->LSU_catch(diff_skip);
    }

    extern void WBU_catch(uint32_t next_pc, \
    uint32_t gpr_waddr, uint32_t gpr_wdata, \
    uint32_t csr_wen, uint32_t csr_waddr, uint32_t csr_wdata){
        emulator->WBU_catch(next_pc, gpr_waddr, gpr_wdata, csr_wen, csr_waddr, csr_wdata);
        sdb->inst_comp();
    }
}
