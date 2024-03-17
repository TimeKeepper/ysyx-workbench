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

#include <isa.h>
#include "local-include/reg.h"
#include <pass_include.h>

const char *regs[] = {
  "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
  "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
  "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
  "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
};

const char *sregs[] = {
  "mstatus", "mtvec", "mepc", "mcause", "mscratch"
};

const int sregs_iddr[] = {
  ADDR_MSTATUS, ADDR_MTVEC, ADDR_MEPC, ADDR_MCAUSE, ADDR_MSCRATCH
};

word_t regs_value_cache[33] = {0};

int store_Regs_Value_cache(int id){
  if(id > 32 || id < 0){
    return -1;
  }
  if(id == 32){
    regs_value_cache[id] = cpu.pc;
    return id;
  }
  regs_value_cache[id] = gpr(id);
  return id;
}

void isa_reg_display(char *reg_name) {
  // if(reg_name!=NULL){
  //   if(strcmp(reg_name, "pc") == 0){
  //     printf("pc: %x\n", cpu.pc);
  //     return;
  //   }
  //   printf("reg: %s val: %x\n", reg_name, isa_reg_str2val(reg_name, NULL));
  //   return;
  // }
  if(reg_name == NULL){
    printf("pc: %x\n", cpu.pc);
    for(int i = 0; i< 32; i++){
      printf("reg: %s val: %x\n", regs[i], gpr(i));
    }
    return;
  }
  if(strcmp(reg_name, "pc") == 0){
    printf("pc: %x\n", cpu.pc);
    return;
  }
  if(strcmp(reg_name, "c") != 0){
    printf("reg: %s val: %x\n", reg_name, isa_reg_str2val(reg_name, NULL));
    return;
  }
  reg_name = strtok(NULL, " ");
  if(reg_name == NULL){
    for(int i = 0; i< 32; i++){
      printf("reg: %s cache_val: %x\n", regs[i], regs_value_cache[i]);
    }
    return;
  }
  if(strcmp(reg_name, "pc") == 0){
    printf("pc cache_val: %x\n", regs_value_cache[32]);
    return;
  }
  for(int i = 0; i< 32; i++){
    if(strcmp(reg_name, regs[i]) == 0){
      printf("reg: %s cache_val: %x\n", regs[i], regs_value_cache[i]);
      return;
    }
  }
}

word_t isa_reg_str2val(const char *s, bool *success) {
  if(success == NULL) {
    Log("The success pointer is NULL, you may consider if there exicts a bug.");goto just_run;
  }
  if(!*success) return 0;
  just_run:
  for(int i = 0; i < 32; i++){
    if(strcmp(regs[i], s) == 0){
      return gpr(i);
    }
  }
  for(int i = 0; i < 5; i++){
    if(strcmp(sregs[i], s) == 0){
      return sr(sregs_iddr[i]);
    }
  }
  Log("The register name is not valid.\n");
  return 0;
}

int isa_str2id(const char *s, bool *success) {
  if(success == NULL) {
    Log("The success pointer is NULL, you may consider if there exicts a bug.\n");goto just_run;
  }
  if(!*success) return 0;
  just_run:
  for(int i = 0; i < 32; i++){
    if(strcmp(regs[i], s) == 0){
      return i;
    }
  }
  return 0;
}

char* isa_id2str(int id) {
  if(id < 0 || id > 32){
    return NULL;
  }
  return (char*)regs[id];
}
