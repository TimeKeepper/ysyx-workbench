/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>

#define LENGTH_OF_BUF 655360

// this should be enough
static char buf[LENGTH_OF_BUF] = {};
static char code_buf[LENGTH_OF_BUF + 128] = {}; // a little larger than `buf`
static char *code_format =
"#include <stdio.h>\n"
"int main() { "
"  unsigned result = %s; "
"  printf(\"%%u\", result); "
"  return 0; "
"}";

uint32_t choose(uint32_t area){
  return rand()%area;
}

static void gen_num() {
  int length_Of_num = choose(3) + 1;
  int length_Of_buf = strlen(buf);
  for(int i = length_Of_buf; i < length_Of_buf + length_Of_num; i++){
    if(i == length_Of_buf) buf[i] = choose(9) + '1';
    else buf[i] = choose(10) + '0';
  }
  buf[length_Of_buf + length_Of_num] = '\0';
}

static void gen(char cha) {
  int length_Of_buf = strlen(buf);
  buf[length_Of_buf] = cha;
  buf[length_Of_buf + 1] = '\0';
}

static void gen_rand_op() {
  switch(choose(4)) {
    case 0: gen('+');break;
    case 1: gen('-');break;
    case 2: gen('*');break;
    case 3: gen('/');break;
    default: assert(0);
  }
}

int token_num = 0;

static bool check_spetial_token_out_of_range(){
  token_num++;
  if(token_num > 10) return true;
  return false;
}

static void gen_rand_expr() {
  check_spetial_token_out_of_range();
  switch (choose(3)) {
    case 0: gen_num(); break;
    case 1: gen('('); gen_rand_expr(); gen(')'); break;
    default: gen_rand_expr(); gen_rand_op(); gen_rand_expr(); break;
  }
}

int main(int argc, char *argv[]) {
  int seed = time(0);
  srand(seed);
  int loop = 1;
  if (argc > 1) {
    sscanf(argv[1], "%d", &loop);
  }
  int i;
  for (i = 0; i < loop; i ++) {
    token_num = 0;
    gen_rand_expr();
    if(check_spetial_token_out_of_range() == true) {
      i--;
      buf[0] = '\0';
      continue;
    }
    // printf("%s\n", buf);

    sprintf(code_buf, code_format, buf);

    FILE *fp = fopen("/tmp/.code.c", "w");
    assert(fp != NULL);
    fputs(code_buf, fp);
    fclose(fp);

    int ret = system("gcc /tmp/.code.c -o /tmp/.expr");
    if (ret != 0) continue;

    fp = popen("/tmp/.expr", "r");
    assert(fp != NULL);

    int result;
    ret = fscanf(fp, "%d", &result);
    pclose(fp);

    printf("%u %s\n", result, buf);

    buf[0] = '\0';
  }
  return 0;
}
