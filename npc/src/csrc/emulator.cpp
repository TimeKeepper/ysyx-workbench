#include <unordered_map>
#include <utils.hpp>
#include <emulator.hpp>

#include <chrono>
#include <getopt.h>

Emulator::Emulator(int argc, char **argv) : argc(argc), argv{argv} {
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
        case 1  : img_file      = optarg;   return;
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

    this->seed = std::chrono::system_clock::now().time_since_epoch().count();

    memorys.emplace("psram", std::make_unique<Memory>(CONFIG_PSRAM_SIZE));
    memorys.emplace("sdram", std::make_unique<Memory>(CONFIG_SDRAM_SIZE));
    memorys.emplace("mrom", std::make_unique<Memory>(CONFIG_MROM_SIZE));
    memorys.emplace("flash", std::make_unique<Memory>(CONFIG_FLASH_SIZE));
    memorys.emplace("vga", std::make_unique<Memory>(CONFIG_VGA_FRAME_BUFFER_SIZE));
    
    this->cpu.sr[ADDR_MVENDORID] = 0x79737978;
    this->cpu.sr[ADDR_MARCHID]   = 23060198;
}

Emulator::~Emulator() {
    TODO();
}

void Emulator::Emulator_mainLoop() {
    TODO();
}
