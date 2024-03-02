#include "cpu/cpu.h"
#include <common.h>
#include <cstdlib>
#include <sdb/sdb.h>
#include <fcntl.h>
#include <gelf.h>
#include <libelf.h>
#include <getopt.h>
#include <memory/paddr.h>

static char* elf_file = NULL;
static char* img_file;

static struct funtion_info {
    char *name;
    long addr;
    long size;
}   funtion_info_table[100];

static int funtion_index = 0;

static void funtion_push(char *name, long addr, long size) {
    funtion_info_table[funtion_index].name = name;
    funtion_info_table[funtion_index].addr = addr;
    funtion_info_table[funtion_index].size = size;
    funtion_index++;
}

static int get_funt_index(long addr){
    for(int i = 0; i < funtion_index; i++){
        if(funtion_info_table[i].addr <= addr && addr < funtion_info_table[i].addr + funtion_info_table[i].size){
            return i;
        }
    }
    return -1;
}

char* get_func_name(long addr){
    int index = get_funt_index(addr);
    if(index == -1){
        return NULL;
    }
    return funtion_info_table[index].name;
}

static long load_elf() {
    Elf *elf;
    Elf_Scn *scn = NULL;
    GElf_Shdr shdr;

    if (elf_file == NULL) {
        printf("No ELF is given. There will no function message.\n");
        return 0;
    }

    int fd;
    if((fd = open(elf_file, O_RDONLY, 0)) < 0){
        printf("Can not open '%s'\n", elf_file);
        return 0;
    }
    if(elf_version(EV_CURRENT) == EV_NONE){
        printf("ELF library initialization failed: %s\n", elf_errmsg(-1));
        return 0;
    }
    if ((elf = elf_begin(fd, ELF_C_READ, NULL)) == NULL){
        printf("elf_begin() failed: %s.\n", elf_errmsg(-1));
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
                    if (name != NULL) {
                        funtion_push(name, sym.st_value, sym.st_size);
                    }
                }
            }
        }
    }
    elf_end(elf);
    return symcount;
}

long load_img(char* img_file) {
    if (img_file == NULL) {
        printf("No image is given. Use the default build-in image.\n");
        return DEFAULT_MSIZE; // built-in image size
    }

    FILE *fp = fopen(img_file, "rb");

    fseek(fp, 0, SEEK_END);
    long size = ftell(fp);

    printf("The image is %s, size = %ld\n", img_file, size);

    fseek(fp, 0, SEEK_SET);
    int ret = fread(get_pmem(), 4, size, fp);
    assert(ret == size/4);

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
        case 'b': break;
        case 'p': break;
        case 'l': break;
        case 'd': break;
        case 'e': elf_file = optarg; break;
        case 1  : img_file = optarg; return 0;
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

void init_monitor(int argc, char *argv[]) {
    parse_args(argc, argv);

    load_img(img_file);

    load_elf();

    init_sdb();

    init_disasm("riscv32");
}