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

#include <common.h>
#include "monitor/sdb/sdb.h"
bool success = true;
extern void init_regex();

void init_monitor(int, char *[]);
void am_init_monitor();
void engine_start();
int is_exit_status_bad();

#define EXPR_TEST
#define MAX_EXPR_LENGTH 65536

int main(int argc, char *argv[]) {
#ifndef EXPR_TEST
  /* Initialize the monitor. */
#ifdef CONFIG_TARGET_AM
  am_init_monitor();
#else
  init_monitor(argc, argv);
#endif

  /* Start engine. */
  engine_start();

  return is_exit_status_bad();
#else
  init_monitor(argc, argv);

  FILE *fp = fopen("../tools/gen-expr/input", "r");

  char buf[MAX_EXPR_LENGTH] = {};

  //一行一行读取fp指定文件的内容
  while(fgets(buf, MAX_EXPR_LENGTH, fp) != NULL){
    char * res_str = strtok(buf, " ");
    int result = atoi(res_str);
    char *e = strtok(buf + strlen(res_str) + 1, "\n");
    if(result != expr(e, &success)){
      printf("Wrong answer! The expression is %s\n", e);
      printf("The result should be %d, but your result is %d\n", result, expr(e, &success));
      return 0;
    }
  }

#endif
}
