使用libelf解析elf文件以进行function trace

在初始化中
```c
#include <libelf.h>

static struct funtion_info {
  char *name;
  long addr;
  long size;
} funtion_info_table[100];

static int funtion_index = 0;

static void funtion_push(char *name, long addr, long size) {
  funtion_info_table[funtion_index].name = name;
  funtion_info_table[funtion_index].addr = addr;
  funtion_info_table[funtion_index].size = size;
  funtion_index++;
}

static int get_funt_index(long addr) {
  for (int i = 0; i < funtion_index; i++) {
    if (funtion_info_table[i].addr <= addr &&
        addr < funtion_info_table[i].addr + funtion_info_table[i].size) {
      return i;
    }
  }
  return -1;
}

char *get_func_name(long addr) {
  int index = get_funt_index(addr);
  if (index == -1) {
    return NULL;
  }
  return funtion_info_table[index].name;
}

static long load_elf() {
  Elf *elf;
  Elf_Scn *scn = NULL;
  GElf_Shdr shdr;

  if (elf_file == NULL) {
    Log("No ELF is given. There will no function message.");
    return 0;
  }

  int fd;
  if ((fd = open(elf_file, O_RDONLY, 0)) < 0) {
    Log("Can not open '%s'", elf_file);
    return 0;
  }
  if (elf_version(EV_CURRENT) == EV_NONE) {
    Log("ELF library initialization failed: %s", elf_errmsg(-1));
    return 0;
  }
  if ((elf = elf_begin(fd, ELF_C_READ, NULL)) == NULL) {
    Log("elf_begin() failed: %s.", elf_errmsg(-1));
    return 0;
  }

  int symcount = 0;

  while ((scn = elf_nextscn(elf, scn)) != NULL) { // 遍历所有section
    gelf_getshdr(scn, &shdr);
    if (shdr.sh_type == SHT_SYMTAB) {
      Elf_Data *data = NULL;
      data = elf_getdata(scn, data);
      symcount = shdr.sh_size / shdr.sh_entsize; // 计算符号数量
      GElf_Sym sym;
      for (int i = 0; i < symcount; i++) {
        gelf_getsym(data, i, &sym);
        if (GELF_ST_TYPE(sym.st_info) == STT_FUNC) { // 只处理函数
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
```

在程序执行中
```c
static bool is_ret = false;
char* get_func_name(long addr);

static void func_called_detect(Decode *s){
  static uint32_t stack_num = 0;

  static char* last_func_name = NULL;
  char* func_name = get_func_name(s->pc);
  if(func_name != NULL && last_func_name != func_name){
    if(is_ret) {printf("ret  "); is_ret = false; stack_num--;}
    else {printf("call "); stack_num++;}

    for(int i = 0; i < stack_num; i++) printf(" ");
    printf("[%s]\n", func_name);
  }
  last_func_name = func_name;
}
```

在执行时关注是否为ret指令
```c
if(s->isa.inst.val == 0x00008067) is_ret = true;
func_called_detect(s);
```