#ifndef __PATH_INCLUDE_H__
#define __PATH_INCLUDE_H__

#include "common.h"

//this funtion help sdb to change the value of register
void change_register_value(int regNO, word_t value);

//this help sdb to store the value of register, which is helpful for debugging
int store_Regs_Value_cache(int id);

//this help display sepecific register when cpu decode
char* isa_id2str(int id);

#endif
