#include "cpu/cpu.h"
#include "utils.h"
#include <common.h>
#include <cstdlib>
#include <sdb/sdb.h>
#include <fcntl.h>
#include <gelf.h>
#include <libelf.h>
#include <getopt.h>
#include <memory/paddr.h>
#include <signal.h>

void init_rand();
void init_mem();

#ifdef PLATFORM_YSYXSOC
#define MSG "YSYXSOC"
#elif defined (PLATFORM_NPC)
#define MSG "NPC"
#endif

static void welcome() {
  printf("Welcome to %s-" MSG "!\n", ANSI_FMT("riscv32e", ANSI_FG_YELLOW));
  printf("For help, type \"help\"\n");
}

static char* elf_file = NULL;
static char* img_file = NULL;
static char* diff_so_file = NULL;
static int difftest_port = 1234;

static struct funtion_info {
    char *name;
    long addr;
    long size;
}   funtion_info_table[1000];

static int funtion_index = 0;

#ifdef CONFIG_FTRACE
static void funtion_push(char *name, long addr, long size) {
    funtion_info_table[funtion_index].name = name;
    funtion_info_table[funtion_index].addr = addr;
    funtion_info_table[funtion_index].size = size;
    funtion_index++;
}
#endif

struct get_func func{NULL, false};

struct get_func get_func_name(long addr){
    for(int i = 0; i < funtion_index; i++){
        if(funtion_info_table[i].addr == addr){
            func.name = funtion_info_table[i].name;
            func.is_call = true;
            return func;
        } else if(funtion_info_table[i].addr < addr && funtion_info_table[i].addr + funtion_info_table[i].size > addr){
            func.name = funtion_info_table[i].name;
            func.is_call = false;
            return func;
        }
    }
    return func;
}

vaddr_t __main_addr__ = 0;

static long load_elf() {
    Elf *elf;
    Elf_Scn *scn = NULL;
    GElf_Shdr shdr;
    #ifdef CONFIG_FTRACE

    if (elf_file == NULL) {
        Log("No ELF is given. There will no function message.");
        return 0;
    }

    int fd;
    if((fd = open(elf_file, O_RDONLY, 0)) < 0){
        Log("Can not open '%s'\n", elf_file);
        return 0;
    }
    if(elf_version(EV_CURRENT) == EV_NONE){
        Log("ELF library initialization failed: %s", elf_errmsg(-1));
        return 0;
    }
    if ((elf = elf_begin(fd, ELF_C_READ, NULL)) == NULL){
        Log("elf_begin() failed: %s.", elf_errmsg(-1));
        return 0;
    }

    int symcount = 0;

    while((scn = elf_nextscn(elf, scn)) != NULL) {
        gelf_getshdr(scn, &shdr);
        if(shdr.sh_type == SHT_SYMTAB) {
            Elf_Data *data = NULL;
            data = elf_getdata(scn, data);
            symcount = shdr.sh_size / shdr.sh_entsize;
            GElf_Sym sym;
            for(int i = 0; i < symcount; i++) {
                gelf_getsym(data, i, &sym);
                if(GELF_ST_TYPE(sym.st_info) == STT_FUNC) {
                    char *name = elf_strptr(elf, shdr.sh_link, sym.st_name);
                    if(strcmp(name, "main") == 0) {__main_addr__ = sym.st_value; }
                    if (name != NULL) {
                        funtion_push(name, sym.st_value, sym.st_size);
                    }
                }
            }
        }
    }
    elf_end(elf);
    Log("Function Trace " ANSI_FMT("ON", ANSI_FG_GREEN));
    return symcount;
    #else
    Log("Function Trace " ANSI_FMT("OFF", ANSI_FG_RED));
    return 0;
    #endif
}

long load_img(char* img_file) {
    if (img_file == NULL) {
        Log("No image is given. Use the default build-in image.");
        return 4096; // built-in image size
    }

    FILE *fp = fopen(img_file, "rb");

    fseek(fp, 0, SEEK_END);
    long size = ftell(fp);

    Log("The image is %s, size = %ld", img_file, size);

    fseek(fp, 0, SEEK_SET);
    int ret = fread(get_loadmem(), size, 1, fp);
    assert(ret == 1);

    fclose(fp);
    return size;
}

static int parse_args(int argc, char *argv[]) {
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
        case 'b': sdb_set_batch_mode();     break;
        case 'p': break;
        case 'l': break;
        case 'd': diff_so_file = optarg;    break;
        case 'e': elf_file = optarg;        break;
        case 1  : img_file = optarg;        return 0;
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
    return 0;
}

void SIGINT_handler(int signal){
    if(signal == SIGINT){
        printf(ANSI_FMT("\nprogram interrupted\n", ANSI_FG_BLUE));
        npc_state.state = NPC_STOP;
    }
}

void init_isa() {
    cpu.sr[ADDR_MVENDORID] = 0x79737978;
    cpu.sr[ADDR_MARCHID] = 23060198;
}

void init_sig(void){
    signal(SIGINT, SIGINT_handler);
}


void init_nvboard(void) {
    #ifdef CONFIG_NVBOARD
    extern TOP_NAME* top;
    void nvboard_bind_all_pins(TOP_NAME* top);  

    nvboard_bind_all_pins(top);

    nvboard_init();
    
    Log("NVBoard " ANSI_FMT("ON", ANSI_FG_GREEN));
    #else
    Log("NVBoard " ANSI_FMT("OFF", ANSI_FG_RED));
    #endif
}

void init_monitor(int argc, char *argv[]) {
    void init_log(void);
    init_log();

    parse_args(argc, argv);

    init_rand();

    init_mem();

    init_isa();

    long img_size = load_img(img_file);

    init_difftest(diff_so_file, img_size, difftest_port);

    init_sdb();

    init_disasm("riscv32");

    load_elf();

    Init_wavetrace(argc, argv);

    init_sig();

    init_nvboard();

    welcome();
}