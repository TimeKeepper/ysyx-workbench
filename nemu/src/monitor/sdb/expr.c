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

#include "common.h"
#include "memory/paddr.h"
#include <isa.h>

/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>

typedef enum {
  TK_NOTYPE = 256, 
  //十进制整数
  TK_DECIMAL,
  //十六进制整数
  TK_HEX,
  //寄存器
  TK_REG,
  //运算符号
  TK_EQ, 
  TK_NEQ,
  TK_PLUS, 
  TK_MINUS,
  TK_MULT,
  TK_DIV,
  //括号
  TK_LPAREN,
  TK_RPAREN,
  //逻辑运算符号
  TK_AND,
  //指针解引用
  TK_DEREF,

  /* TODO: Add more token types */

} TokenType;

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {

  /* TODO: Add more rules.
   * Pay attention to the precedence level of different rules.
   */

  {" +", TK_NOTYPE},    // spaces
  {"\\+", TK_PLUS},         // plus,escape the escape character itself and use escaped escape character to escape the character '+' which need to be escaped.
  {"-", TK_MINUS},         // minus
  {"\\*", TK_MULT},         // multiply or dereference
  {"/", TK_DIV},         // divide
  {"\\(", TK_LPAREN},         // left parenthesis
  {"\\)", TK_RPAREN},         // right parenthesis
  {"\\$[\\$]?[0-9a-zA-Z]+",TK_REG},         // register
  {"0[xX][0-9a-fA-F]+", TK_HEX},         // hex
  {"[0-9]+", TK_DECIMAL},         // decimal
  {"==", TK_EQ},        // equal
  {"!=", TK_NEQ},       // non-equal
  {"&&", TK_AND},       // and
};

#define NR_REGEX ARRLEN(rules)

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex() {
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i ++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) {
      regerror(ret, &re[i], error_msg, 128);
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token {
  int type;
  char str[32];
} Token;

static Token tokens[32] __attribute__((used)) = {};
static int nr_token __attribute__((used))  = 0;

static bool make_token(char *e) {
  int position = 0;
  int i;
  regmatch_t pmatch;

  nr_token = 0;

  while (e[position] != '\0') {
    /* Try all rules one by one. */
    for (i = 0; i < NR_REGEX; i ++) {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0) {
        char *substr_start = e + position;
        int substr_len = pmatch.rm_eo;

        Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
            i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        /* TODO: Now a new token is recognized with rules[i]. Add codes
         * to record the token in the array `tokens'. For certain types
         * of tokens, some extra actions should be performed.
         */

        switch (rules[i].token_type) {
          //运算符号
          case TK_PLUS:   tokens[nr_token++].type = TK_PLUS; break;
          case TK_MINUS:  tokens[nr_token++].type = TK_MINUS; break;
          case TK_MULT:   tokens[nr_token++].type = TK_MULT; break;
          case TK_DIV:    tokens[nr_token++].type = TK_DIV; break;
          //括号
          case TK_LPAREN: tokens[nr_token++].type = TK_LPAREN; break;
          case TK_RPAREN: tokens[nr_token++].type = TK_RPAREN; break;
          //十进制整数
          case TK_DECIMAL: {
            tokens[nr_token].type = TK_DECIMAL;
            if (substr_len >= 32) {
              printf("decimal number is too long!\n");
              return false;
            }
            else {
              strncpy(tokens[nr_token].str, substr_start, substr_len);
              tokens[nr_token].str[substr_len] = '\0';
              nr_token++;
            }
            break;
          }
          //十六进制整数
          case TK_HEX: {
            tokens[nr_token].type = TK_HEX;
            if (substr_len >= 32) {
              printf("hex number is too long!\n");
              return false;
            }
            else {
              strncpy(tokens[nr_token].str, substr_start, substr_len);
              tokens[nr_token].str[substr_len] = '\0';
              nr_token++;
            }
            break;
          }
          //寄存器
          case TK_REG: {
            tokens[nr_token].type = TK_REG;
            if (substr_len >= 32) {
              printf("register name is too long!\n");
              return false;
            }
            else {
              strncpy(tokens[nr_token].str, substr_start, substr_len);
              tokens[nr_token].str[substr_len] = '\0';
              nr_token++;
            }
            break;
          }
          //等号
          case TK_EQ:     tokens[nr_token++].type = TK_EQ; break;
          //不等号
          case TK_NEQ:    tokens[nr_token++].type = TK_NEQ; break;
          //逻辑运算符号
          case TK_AND:    tokens[nr_token++].type = TK_AND; break;
          //空格
          case TK_NOTYPE: break;
          default: Log("You have type some unknown token!"); return false;
        }

        break;
      }
    }

    if (i == NR_REGEX) {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }

  return true;
}

static bool check_Parenmatch(int p, int q){
  int i;
  int op = 0;
  for(i = p; i <= q; i++){
    if(tokens[i].type == TK_LPAREN){
      op++;
    }
    else if(tokens[i].type == TK_RPAREN){
      op--;
    }
    if(op < 0){
      return false;
    }
  }
  if(op == 0){
    return true;
  }
  else{
    return false;
  }
}

static bool  check_Parentheses(int p, int q){
  if(tokens[p].type == TK_LPAREN && tokens[q].type == TK_RPAREN){
    return check_Parenmatch(p + 1, q - 1);
  }
  else{
    return false;
  }
}

static int find_Op(int p, int q){
  int i;
  int op = 0;
  int op_type = 0;
  for(i = p; i <= q; i++){
    switch (tokens[i].type)
    {
    case TK_LPAREN:
      op++;
      break;
    case TK_RPAREN:
      op--;
      break;
    case TK_DEREF:
      if(op == 0 && (op_type == 0)){
        op_type = i;
      }
      break;
    case TK_MULT:
    case TK_DIV:
      if(op == 0 && (op_type == 0 || tokens[op_type].type == TK_MULT || tokens[op_type].type == TK_DIV)){
        op_type = i;
      }
      break;
    case TK_PLUS:
    case TK_MINUS:
      if(op == 0 && (op_type == 0 || tokens[op_type].type == TK_PLUS || tokens[op_type].type == TK_MINUS || \
      tokens[op_type].type == TK_MULT || tokens[op_type].type == TK_DIV)){ 
        op_type = i;
      }
      break;
    case TK_EQ:
    case TK_NEQ:
      if(op == 0 && (op_type == 0 || tokens[op_type].type == TK_EQ || tokens[op_type].type == TK_NEQ || \
      tokens[op_type].type == TK_PLUS || tokens[op_type].type == TK_MINUS || tokens[op_type].type == TK_MULT || tokens[op_type].type == TK_DIV)){
        op_type = i;
      }
      break;
    case TK_AND:
      if(op == 0 && (op_type == 0 || tokens[op_type].type == TK_AND || tokens[op_type].type == TK_EQ || tokens[op_type].type == TK_NEQ || \
      tokens[op_type].type == TK_PLUS || tokens[op_type].type == TK_MINUS || tokens[op_type].type == TK_MULT || tokens[op_type].type == TK_DIV)){
        op_type = i;
      }
      break;
    default:
      break;
    }
  }
  return op_type;
}

word_t eval(int p, int q, bool *success){
  if(check_Parenmatch(p, q) == false) {
    *success = false;
    return 0;
  }
  if(*success == false) return 0;
  else if(p > q){
    *success = false;
    return 0;
  }
  else if(p == q){
    if(tokens[p].type == TK_DECIMAL){
      return atoi(tokens[p].str);
    }
    else if(tokens[p].type == TK_HEX){
      return strtol(tokens[p].str, NULL, 16);
    }
    else if(tokens[p].type == TK_REG){
      if(strcmp(tokens[p].str, "pc") == 0) return cpu.pc;
      return isa_reg_str2val(tokens[p].str+1, success);
    }
    else{
      *success = false;
      return 0;
    }
  }
  else if(check_Parentheses(p , q) == true) {
    return eval(p + 1, q - 1, success);
  }
  else {
    word_t val1, val2;
    
    int index = find_Op(p, q);
    val2 = eval(index + 1, q, success);
    if(tokens[index].type == TK_DEREF)
      return paddr_read(val2, 4);
    
    val1 = eval(p, index - 1, success);

    switch(tokens[index].type){
      case TK_PLUS: return val1 + val2;
      case TK_MINUS: return val1 - val2;
      case TK_MULT: return val1 * val2;
      case TK_DIV: return val2 == 0 ? 0: val1 / val2;
      case TK_EQ: return val1 == val2;
      case TK_NEQ: return val1 != val2;
      case TK_AND: return val1 && val2;
      default: panic("Unknown condition, you should check you code again!");
    }
  }
}

word_t expr(char *e, bool *success) {
  if (!make_token(e)) {
    *success = false;
    return 0;
  }

  for (int i = 0; i < nr_token; i ++) {
  if (tokens[i].type == TK_MULT && (i == 0 || tokens[i - 1].type == TK_AND || tokens[i - 1].type == TK_EQ || tokens[i - 1].type == TK_NEQ || \
  tokens[i - 1].type == TK_PLUS || tokens[i - 1].type == TK_MINUS || tokens[i - 1].type == TK_MULT || tokens[i - 1].type == TK_DIV || \
  tokens[i - 1].type == TK_LPAREN || tokens[i - 1].type == TK_DEREF)) {
    tokens[i].type = TK_DEREF;
  }
}

  return eval(0, nr_token-1, success);
}
