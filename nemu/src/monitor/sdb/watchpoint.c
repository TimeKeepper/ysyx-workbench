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

#include "sdb.h"

#define NR_WP 32

static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;

WP* new_wp(char* exp){
  if(exp == NULL){
    printf(ANSI_FMT("No expression!\n", ANSI_FG_RED));
    assert(0);
  }
  if(free_ == NULL){
    printf(ANSI_FMT("No more watchpoint!\n", ANSI_FG_RED));
    assert(0);
  }
  WP *p = free_;
  free_ = free_->next;
  p->next = head;
  head = p;
  p->expr = malloc(strlen(exp)+1);
  strcpy(p->expr, exp);
  bool success = true;
  p->value = expr(exp, &success);
  return p;
}

WP* get_head_wp(void){
  return head;
}

void free_wp(WP *wp){
  if(wp == NULL){
    printf("No such watchpoint!\n");
    assert(0);
  }
  free(wp->expr);
  if(wp == head){
    head = head->next;
  }
  else{
    WP *p = head;
    while(p->next != wp){
      p = p->next;
    }
    p->next = wp->next;
  }
  wp->next = free_;
  free_ = wp;
}
 
void wp_display(void){
  WP *p = head;
  while(p != NULL){
    printf("watchpoint %d: (" ANSI_FMT("%s", ANSI_FG_BLUE) ") = 0x%08x\n", p->NO, p->expr, p->value);
    p = p->next;
  }
}

void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i ++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
  }

  head = NULL;
  free_ = wp_pool;
}

void wp_Value_Update(){
  WP *p = head;
  while(p != NULL){
    bool success = true;
    p->last_time_Value = p->value;
    p->value = expr(p->expr, &success);
    p = p->next;
  }
}

WP* get_Changed_wp(int num){
  WP *p = head;
  int i = 0;
  while(p != NULL){
    if(p->last_time_Value != p->value){
      if(i++ == num)
        return p;
    }
    p = p->next;
  }
  return NULL;
}

/* TODO: Implement the functionality of watchpoint */

