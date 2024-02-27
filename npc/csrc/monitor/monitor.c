#include <assert.h>
#include <cstdlib>
#include <monitor.h>
#include <fcntl.h>
#include <gelf.h>
#include <libelf.h>
#include <getopt.h>
#include <stdio.h>
#include <img.h>

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
      case 'e': break;
      case 1: img_file = optarg; return 0;
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

static long load_img(uint32_t *img_ram) {
  if (img_file == NULL) {
    printf("No image is given. Use the default build-in image.");
    return 4096; // built-in image size
  }

  FILE *fp = fopen(img_file, "rb");

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);

  printf("The image is %s, size = %ld", img_file, size);

  fseek(fp, 0, SEEK_SET);
  int ret = fread(img_ram, 4, size, fp);
  assert(ret == size/4);

    for(int i = 0; i < size/4; i++){
        printf("%x\n", img_ram[i]);
    }

  fclose(fp);
  return size;
}

void init_monitor(int argc, char *argv[], uint32_t *img_ram) {
    parse_args(argc, argv);

    load_img(img_ram);
}