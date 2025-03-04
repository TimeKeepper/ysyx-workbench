#include "performence.hpp"
#include <iomanip>
#include <simple_debugger.hpp>
#include <memory>
#include <utils.hpp>
#include <emulator.hpp>

extern Emulator* emulator;
extern simple_debugger* sdb;

extern "C" {

#ifdef CONFIG_PLATFORM_YSYXSOC
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
        // std::cout << "sdram_write: " 
        // << std::hex << std::setw(8) << std::setfill('0')
        // << waddr + emulator->memorys["sdram"]->base << " " 
        // << std::hex << std::setw(4) << std::setfill('0')
        // << wdata << " " << wlen 
        // << std::dec << std::endl;
        emulator->memorys["sdram"]->write(waddr, wlen, wdata);
    }

    extern void sdram_read(int32_t addr, int32_t* data) {
        // std::cout << "sdram_read: " << addr << std::endl;
        *data = emulator->memorys["sdram"]->read(addr, 2);
    }

    extern void vga_write(int32_t waddr, int32_t wdata) {
        emulator->memorys["vga"]->write(waddr - CONFIG_VGA_FRAME_BUFFER_BASE, 4, wdata);
    }

    extern void vga_read(int32_t x_addr, int32_t y_addr, int32_t* rdata) {
        uint32_t raddr = (y_addr * 640 + x_addr) * 4;
        *rdata = emulator->memorys["vga"]->read((raddr & ~0x3u), 4);
    }
    
#elif defined (CONFIG_PLATFORM_NPC)
    extern void sram_read(int32_t addr, int32_t* data) {
        *data = emulator->memorys["sram"]->read((addr & ~0x3u) - CONFIG_LOAD_MEMORY_BASE, 4);
    }

    extern void sram_write(int32_t addr, int32_t data, int32_t strb){
        int32_t len;
        switch(strb){
            case 0b0001:
            case 0b0010:
            case 0b0100: 
            case 0b1000: len = 1; break;
            case 0b0011:
            case 0b1100: len = 2; break;
            case 0b1111: len = 4; break;
            default: Assert(0, "Invalid strb: %d", strb);
        }

        switch(strb){
            case 0b0010: data >>= 8; break;
            case 0b0100:
            case 0b1100: data >>= 16; break;
            case 0b1000: data >>= 24; break;
            default: break;
        }

        emulator->memorys["sram"]->write(addr - CONFIG_LOAD_MEMORY_BASE, len, data);
    }
    
    extern void Uart_putc(int32_t ch){
        std::cout << (char)ch;
        std::cout.flush();
    }
#endif

    extern void IFU_catch(uint32_t inst){
        emulator->IFU_catch(inst);
    }

    extern void Icache_catch(uint32_t map_hit, uint32_t cache_hit){
        emulator->Icache_catch(map_hit, cache_hit);
    }

    extern void Icache_state_catch(uint32_t write_index, uint32_t write_way, uint32_t write_tag, const svBitVecVal* write_data) {
        emulator->Icache_state_catch(write_index, write_way, write_tag, write_data);
    }

    extern void Icache_flush() {
        emulator->Icache_flush();
    }

    extern void Icache_MAT_catch(uint32_t count) {
        emulator->Icache_MAT_catch(count);
    }

    extern void IDU_catch(uint32_t type){
        performence::Inst_Type inst_type;
        switch(type){
            case 0: inst_type = performence::Inst_Type::Cal; break;
            case 1: inst_type = performence::Inst_Type::LS; break;
            case 2: inst_type = performence::Inst_Type::CSR; break;
            default: inst_type = performence::Inst_Type::GP; break;
        }
        emulator->IDU_catch(inst_type);
    }

    extern void ALU_catch(){
        emulator->ALU_catch();
    }

    extern void LSU_catch(uint32_t diff_skip){
        emulator->LSU_catch();
        sdb->LSU_catch(diff_skip);
    }

    extern void WBU_catch(uint32_t next_pc, \
    uint32_t gpr_waddr, uint32_t gpr_wdata, \
    uint32_t csr_wena, uint32_t csr_waddra, uint32_t csr_wdataa, \
    uint32_t csr_wenb, uint32_t csr_waddrb, uint32_t csr_wdatab){
        emulator->WBU_catch(next_pc, gpr_waddr, gpr_wdata, csr_wena, csr_waddra, csr_wdataa, csr_wenb, csr_waddrb, csr_wdatab);
        sdb->WBU_catch();
    }
}
