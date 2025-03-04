#include "common.hpp"
#include "cpu.hpp"
#include "memory.hpp"
#include "svdpi.h"
#include <cstdint>
#include <iomanip>
#include <iostream>
#include <string>
#include <unordered_map>
#include <utility>
#include <utils.hpp>
#include <sstream>
#include <emulator.hpp>

#include <chrono>
#include <getopt.h>

std::map<uint32_t, std::string> csr_key = {
    {ADDR_MSTATUS, "mstatus"},
    {ADDR_MTVEC, "mtvec"},
    {ADDR_MEPC, "mepc"},
    {ADDR_MCAUSE, "mcause"},
    {ADDR_MSCRATCH, "mscratch"}
};

std::map<uint8_t, std::string> gpr_key = {
    {0, "zero"},    {1, "ra"},      {2, "sp"},      {3, "gp"}, 
    {4, "tp"},      {5, "t0"},      {6, "t1"},      {7, "t2"}, 
    {8, "s0"},      {9, "s1"},      {10, "a0"},     {11, "a1"}, 
    {12, "a2"},     {13, "a3"},     {14, "a4"},     {15, "a5"}, 
    {16, "a6"},     {17, "a7"},     {18, "s2"},     {19, "s3"}, 
    {20, "s4"},     {21, "s5"},     {22, "s6"},     {23, "s7"}, 
    {24, "s8"},     {25, "s9"},     {26, "s10"},    {27, "s11"}, 
    {28, "t3"},     {29, "t4"},     {30, "t5"},     {31, "t6"},
};

const int gpr_name2id(const std::string& name){
    for(auto &i : gpr_key){
        if(i.second == name){
            return i.first;
        }
    }
    return -1;
}

const char* gpr_id2name(int id){
    auto it = gpr_key.find(id);
    if(it != gpr_key.end()){
        return (char*)it->second.c_str();
    }
    return "Unknown";
}

const char* csr_id2name(int id){
  auto it = csr_key.find(id);
  if(it != csr_key.end()){
    return (char*)it->second.c_str();
  }
  static std::string id_str;
  id_str = std::to_string(id);
  return id_str.c_str();
}

void Emulator::wave_trace_once(){
    this->contextp->timeInc(1);
    this->tfp->dump(contextp->time());
}

void Emulator::instruction_buffer_push(uint32_t pc, uint32_t inst){
    if(this->instruction_buffer.size() == this->buffer_cap){
        this->instruction_buffer.pop_front();
    }
    this->instruction_buffer.emplace_back(pc, inst);
}

void Emulator::instruction_buffer_print(){
    for(auto &i : this->instruction_buffer){
        std::cout << this->disasm(i.first, i.second) << std::endl;
    }
}

std::string Emulator::disasm(uint32_t pc, uint32_t inst){
    std::stringstream ss;
    ss << ANSI_FG_CYAN << "0x" << std::hex << std::nouppercase 
        << std::setw(8) << std::setfill('0') << pc << '\t' 
        << ANSI_FG_YELLOW << std::setw(8) << std::setfill('0') << inst 
        << ANSI_NONE;

    char inst_str[64];

    void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
    disassemble(inst_str, 64, pc, (uint8_t*)&inst, 4);

    return ss.str() + '\t' + ANSI_FG_BLUE + inst_str + ANSI_NONE;
}

void Emulator::parse_args() {
    const struct option table[] = {
      {"batch"    , no_argument      , NULL, 'b'},
      {"log"      , required_argument, NULL, 'l'},
      {"diff"     , required_argument, NULL, 'd'},
      {"port"     , required_argument, NULL, 'p'},
      {"elf"      , required_argument, NULL, 'e'},
      {"help"     , no_argument      , NULL, 'h'},
      {0          , 0                , NULL,  0 },
    };

    int o;
    while ( (o = getopt_long(argc, argv, "-bhl:d:p:e:", table, NULL)) != -1) {
        switch (o) {
            case 'b': is_batch_mode = true;     break;
            case 'p':                           break;
            case 'l':                           break;
            case 'd': diff_so_file  = optarg;   break;
            case 'e': elf_file      = optarg;   break;
            case 1  : img_file      = optarg;   {return;}
            default:
            printf("Usage: %s [OPTION...] IMAGE [args]\n\n", argv[0]);
            printf("\t-b,--batch              run with batch mode\n");
            printf("\t-l,--log=FILE           output log to FILE\n");
            printf("\t-d,--diff=REF_SO        run DiffTest with reference REF_SO\n");
            printf("\t-p,--port=PORT          run DiffTest with port PORT\n");
            printf("\n");
            exit(0);
        }
    }
}

void Emulator::init_rand() {
    this->seed = std::chrono::system_clock::now().time_since_epoch().count();
    std::srand(seed);
}

void Emulator::init_mem() {
    #ifdef CONFIG_waveForm_vcd
    std::cout << "vcd" << std::endl;
    #endif

    #ifdef CONFIG_PLATFORM_YSYXSOC
    memorys.emplace("psram", std::make_unique<Memory>(CONFIG_PSRAM_BASE, CONFIG_PSRAM_SIZE));
    memorys.emplace("sdram", std::make_unique<Memory>(CONFIG_SDRAM_BASE, CONFIG_SDRAM_SIZE));
    memorys.emplace("mrom", std::make_unique<Memory>(CONFIG_MROM_BASE, CONFIG_MROM_SIZE));
    memorys.emplace("flash", std::make_unique<Memory>(CONFIG_FLASH_BASE, CONFIG_FLASH_SIZE, Memory::Little_endian));
    memorys.emplace("vga", std::make_unique<Memory>(CONFIG_VGA_FRAME_BUFFER_BASE, CONFIG_VGA_FRAME_BUFFER_SIZE));
    #elif defined (CONFIG_PLATFORM_NPC)
    memorys.emplace("sram", std::make_unique<Memory>(CONFIG_LOAD_MEMORY_BASE, CONFIG_LOAD_MEMORY_SIZE));
    #endif

    cache.resize(CONFIG_ICache_Set, std::vector<CacheLine>(CONFIG_ICache_Way));
}

void Emulator::init_isa() {
    this->cpu.pc = CONFIG_RESET_VECTOR;
    this->cpu.sr[ADDR_MVENDORID] = 0x79737978;
    this->cpu.sr[ADDR_MARCHID]   = 23060198;
}

void Emulator::load_image() {
    if (this->img_file == NULL) {
        Log(ANSI_FMT("No image is given. Use the default build-in image.", ANSI_FG_RED));
        return;
    }

    FILE *fp = fopen(this->img_file, "rb");

    fseek(fp, 0, SEEK_END);
    uint64_t size = ftell(fp);

    Log("The image is %s, size = %ld", this->img_file, size);

    fseek(fp, 0, SEEK_SET);
    #ifdef CONFIG_PLATFORM_YSYXSOC
    int ret = fread(this->memorys["flash"]->get_memory(), size, 1, fp);
    #elif defined (CONFIG_PLATFORM_NPC)
    int ret = fread(this->memorys["sram"]->get_memory(), size, 1, fp);
    #endif
    assert(ret == 1);

    fclose(fp);
    this->img_size = size;
}

void Emulator::init_simulate(){
    this->contextp->commandArgs(this->argc, this->argv);

    Verilated::traceEverOn(true);
    this->top->trace(tfp, 99);
    this->tfp->open("wave.vcd");

    #ifdef CONFIG_NVBOARD
    void nvboard_bind_all_pins(TOP_NAME* top);  
    nvboard_bind_all_pins(this->top);
    nvboard_init();
    Log("NVBoard " ANSI_FMT("ON", ANSI_FG_GREEN));
    #endif

    this->reset(20);
}

static void welcome() {
    std::cout << "Welcome to " << ANSI_FMT("riscv32e", ANSI_FG_YELLOW) << "-npc" << std::endl;
    std::cout << "For help, Type 'help'" << std::endl;
}

void init_disasm(const char *triple);

Emulator::Emulator(int argc, char **argv) : argc(argc), argv{argv} {
    this->parse_args();

    this->init_rand();

    this->init_mem();
    
    this->init_isa();

    this->load_image();

    init_disasm("riscv32");

    this->perf = std::make_unique<performence>();

    this->init_simulate();

    welcome();
}

Emulator::~Emulator() {
    this->tfp->close();

    this->memorys.clear();

    #ifdef CONFIG_NVBOARD
    nvboard_quit();
    #endif
}

void Emulator::reset(uint64_t n) {
    this->top->reset = 1;
    cycle(n);
    this->top->reset = 0;
}

void Emulator::cycle(uint64_t n) {
    this->npc_state.state = NPC_RUNNING;

    for(;n > 0; n--) {
        this->top->clock = 0; top->eval();
        if(this->wave_trace_on) wave_trace_once();                  

        this->top->clock = 1; top->eval();
        if(this->wave_trace_on) wave_trace_once();  

        #ifdef CONFIG_NVBOARD
        nvboard_update();
        #endif

        this->perf->clk_count();

        if(this->npc_state.state != NPC_RUNNING) break;
    }

    this->npc_state.state = this->npc_state.state == NPC_RUNNING ? NPC_STOP : this->npc_state.state;
}

const std::pair<const std::string, std::unique_ptr<Memory>>* Emulator::find_match_memory(uint32_t addr){
    for(auto &i : this->memorys){
        if(i.second->match(addr)){
            return &i;
        }
    }
    return nullptr;
}

uint32_t Emulator::memory_read(uint32_t addr){
    auto match_memory = this->find_match_memory(addr);
    if(match_memory == nullptr) return 0;
    return match_memory->second->read_WithBias(addr, 4);
}

void Emulator::single_inst(uint64_t n){
    this->run_inst_num = n;
    this->npc_state.state = NPC_RUNNING;
    while(1){
        this->cycle(1);

        if(this->run_inst_num == 0) break;
    }
}

void Emulator::wave_trace_ctrl(bool v){
    std::cout << "Wave Trace " << (v ? ANSI_FG_GREEN : ANSI_FG_RED)
    << (v ? "ON" : "OFF") << ANSI_NONE << std::endl;
    this->wave_trace_on = v;
}

void Emulator::instruction_trace_ctrl(bool v){
    std::cout << "Instruction Trace " << (v ? ANSI_FG_GREEN : ANSI_FG_RED)
    << (v ? "ON" : "OFF") << ANSI_NONE << std::endl;
    this->instruciton_trace_on = v;
}

void Emulator::Emulator_trap(uint32_t a0) {
    this->npc_state.state = NPC_STOP;
    this->npc_state.halt_ret = a0;

    std::cout << ((a0 == 0) ? \
        ANSI_FMT("Hit good trap", ANSI_FG_GREEN) : \
        ANSI_FMT("Hit bad trap",  ANSI_FG_RED)) << std::endl;
}

void Emulator::IFU_catch(uint32_t pc, uint32_t inst){
    switch(inst){
        case 0x00000000: this->Emulator_trap(1);   break; // ecall
        case 0xffffffff: this->Emulator_trap(1);   break; // bad trap
        case 0x00100073: this->Emulator_trap(cpu.gpr[10]);   break; // ebreak
        default: break;
    }

    this->perf->coponent_count("IFU");

    if(!this->instruciton_trace_on) return;

    this->Inst_quene.push(std::make_pair(pc, inst));
}

void Emulator::Icache_catch(uint32_t map_hit, uint32_t cache_hit){
    this->perf->cache_count("Inst", map_hit!=0, cache_hit!=0);
    this->icache_msg_transmiter.push({map_hit!=0, cache_hit!=0});
}

void Emulator::Icache_state_catch(uint32_t write_index, uint32_t write_way, uint32_t write_tag, const svBitVecVal* write_data) {
    cache[write_index][write_way].tag = write_tag;
    uint32_t index = 0;
    for (uint32_t& k : cache[write_index][write_way].inst) {
        k = write_data[index++];
    }

    cache[write_index][write_way].valid = true;
}

void Emulator::Icache_flush() {
    Log("Flush Icache");
    for (auto& i : cache) {
        for (auto& j : i) {
            j.valid = false;
        }
    }
}

void Emulator::Icache_MAT_catch(uint32_t count){
    this->perf->memory_access_time += count;
}

void Emulator::IDU_catch(performence::Inst_Type type){
    this->perf->inst_type_set(type);
}

void Emulator::ALU_catch(){
    this->perf->coponent_count("ALU");
}

void Emulator::LSU_catch(){
    this->perf->coponent_count("LSU");
}

void Emulator::WBU_catch(uint32_t next_pc, \
    uint32_t gpr_waddr, uint32_t gpr_wdata, \
    uint32_t csr_wena, uint32_t csr_waddra, uint32_t csr_wdataa, \
    uint32_t csr_wenb, uint32_t csr_waddrb, uint32_t csr_wdatab){
        
    this->run_inst_num = (this->run_inst_num == 0) ? 0 : this->run_inst_num - 1;

    this->cpu.pc = next_pc;
    if(gpr_waddr != 0) this->cpu.gpr[gpr_waddr] = gpr_wdata;
    if(csr_wena) this->cpu.sr[csr_waddra] = csr_wdataa;
    if(csr_wenb) this->cpu.sr[csr_waddrb] = csr_wdatab;

    this->perf->inst_cont();

    if(Inst_quene.size() == 0) return;
    
    this->instruction_buffer_push(Inst_quene.front().first, Inst_quene.front().second);

    if(!this->instruciton_trace_on) return;
    std::cout << this->disasm(Inst_quene.front().first, Inst_quene.front().second) << std::endl;
    Inst_quene.pop();
}
