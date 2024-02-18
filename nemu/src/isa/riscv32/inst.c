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

#include "local-include/reg.h"
#include <cpu/cpu.h>
#include <cpu/ifetch.h>
#include <cpu/decode.h>
#include <pass_include.h>

#define R(i) gpr(store_Regs_Value_cache(i))
#define Print_rd (printf("rd:%s,",isa_id2str(rd)))
#define Print_insut_name(name) printf("insut:%s\n,",name)
#define Print_DBG_Message(name) (printf("imm:%x,",imm),(Print_rd,Print_insut_name(name)))
#define Mr vaddr_read
#define Mw vaddr_write

enum {
  TYPE_I, TYPE_U, TYPE_S,TYPE_R,TYPE_B,
  TYPE_J, 
  TYPE_N, // none
};

#define src1R() do { *src1 = R(rs1); printf("src1:%s,",isa_id2str(rs1));} while (0)
#define src2R() do { *src2 = R(rs2); printf("src2:%s,",isa_id2str(rs2));} while (0)
#define immI() do { *imm = SEXT(BITS(i, 31, 20), 12); } while(0)
#define immU() do { *imm = SEXT(BITS(i, 31, 12), 20) << 12; } while(0)
#define immS() do { *imm = SEXT((BITS(i, 31, 25) << 5) | BITS(i, 11, 7), 12); } while(0)
#define immB() do { *imm = SEXT((BITS(i, 31, 31) << 12) | (BITS(i, 30, 25) << 5) | (BITS(i, 11, 8) << 1) | (BITS(i, 7, 7) << 11), 13); } while(0)
#define immJ() do { *imm = SEXT((BITS(i, 31, 31) << 20) | (BITS(i, 30, 21) << 1) | (BITS(i, 20, 20) << 11) | (BITS(i, 19, 12) << 12), 21); } while(0)

static void decode_operand(Decode *s, int *rd, word_t *src1, word_t *src2, word_t *imm, int type) {
  uint32_t i = s->isa.inst.val;
  int rs1 = BITS(i, 19, 15);
  int rs2 = BITS(i, 24, 20);
  *rd     = BITS(i, 11, 7);
  switch (type) {
    case TYPE_I: src1R();          immI(); break;
    case TYPE_U:                   immU(); break;
    case TYPE_S: src1R(); src2R(); immS(); break;
    case TYPE_R: src1R(); src2R();         break;
    case TYPE_B: src1R(); src2R(); immB(); break;
    case TYPE_J:                   immJ(); break;
  }
  // if(type!=TYPE_R) printf("imm:%x,",*imm);
}

static int decode_exec(Decode *s) {
  int rd = 0;
  word_t src1 = 0, src2 = 0, imm = 0;
  s->dnpc = s->snpc;

#define INSTPAT_INST(s) ((s)->isa.inst.val)
#define INSTPAT_MATCH(s, name, type, ... /* execute body */ ) { \
  decode_operand(s, &rd, &src1, &src2, &imm, concat(TYPE_, type)); \
  __VA_ARGS__ ; \
}

  store_Regs_Value_cache(32);

  INSTPAT_START();
  INSTPAT("??????? ????? ????? ??? ????? 01101 11", lui    , U, Print_DBG_Message("lui"),R(rd) = imm);
  INSTPAT("??????? ????? ????? ??? ????? 00101 11", auipc  , U, Print_DBG_Message("auipc"),R(rd) = s->pc + imm);
  INSTPAT("??????? ????? ????? ??? ????? 11011 11", jal    , J, Print_DBG_Message("jal"),R(rd) = s->snpc; s->dnpc += imm - 4);
  INSTPAT("??????? ????? ????? ??? ????? 11001 11", jalr   , I, Print_DBG_Message("jalr"),R(rd) = s->snpc; s->dnpc = (src1 + imm) & ~1);
  INSTPAT("??????? ????? ????? 100 ????? 00000 11", lbu    , I, Print_DBG_Message("lbu"),R(rd) = Mr(src1 + imm, 1));
  INSTPAT("??????? ????? ????? 000 ????? 00100 11", addi   , I, Print_DBG_Message("addi"),R(rd) = src1 + imm);
  INSTPAT("??????? ????? ????? 010 ????? 00100 11", slti   , I, Print_DBG_Message("slti"),R(rd) = ((sword_t)src1 < (sword_t)imm) ? 1 : 0);
  INSTPAT("??????? ????? ????? 011 ????? 00100 11", sltiu  , I, Print_DBG_Message("sltiu"),R(rd) = (src1 < imm) ? 1 : 0);
  INSTPAT("??????? ????? ????? 111 ????? 00100 11", andi   , I, Print_DBG_Message("andi"),R(rd) = src1 & imm);
  INSTPAT("??????? ????? ????? 100 ????? 00100 11", xori   , I, Print_DBG_Message("xori"),R(rd) = src1 ^ imm);
  INSTPAT("??????? ????? ????? 001 ????? 00000 11", lh     , I, Print_DBG_Message("lh"),R(rd) = Mr(src1 + imm, 2));
  INSTPAT("??????? ????? ????? 010 ????? 00000 11", lw     , I, Print_DBG_Message("lw"),R(rd) = Mr(src1 + imm, 4));
  INSTPAT("0000000 ????? ????? 001 ????? 00100 11", slli   , I, imm ^= 0x00000fe0,Print_DBG_Message("slli"),R(rd) = src1 << imm);
  INSTPAT("0000000 ????? ????? 101 ????? 00100 11", srli   , I, imm ^= 0x00000fe0,Print_DBG_Message("srli"),R(rd) = src1 >> imm);
  INSTPAT("0100000 ????? ????? 101 ????? 00100 11", srai   , I, imm ^= 0x00000fe0,Print_DBG_Message("srai"),R(rd) = (sword_t)src1 >> imm);
  INSTPAT("??????? ????? ????? 000 ????? 01000 11", sb     , S, Print_DBG_Message("sb"),Mw(src1 + imm, 1, src2));
  INSTPAT("??????? ????? ????? 001 ????? 01000 11", sh     , S, Print_DBG_Message("sh"),Mw(src1 + imm, 2, src2));
  INSTPAT("??????? ????? ????? 010 ????? 01000 11", sw     , S, Print_DBG_Message("sw"),Mw(src1 + imm, 4, src2));
  INSTPAT("0000000 ????? ????? 000 ????? 01100 11", add    , R, Print_DBG_Message("add"),R(rd) = src1 + src2);
  INSTPAT("0100000 ????? ????? 000 ????? 01100 11", sub    , R, Print_DBG_Message("sub"),R(rd) = src1 - src2);
  INSTPAT("0000000 ????? ????? 111 ????? 01100 11", and    , R, Print_DBG_Message("and"),R(rd) = src1 & src2);
  INSTPAT("0000000 ????? ????? 110 ????? 01100 11", or     , R, Print_DBG_Message("or"),R(rd) = src1 | src2);
  INSTPAT("0000000 ????? ????? 100 ????? 01100 11", xor    , R, Print_DBG_Message("xor"),R(rd) = src1 ^ src2);
  INSTPAT("0000000 ????? ????? 001 ????? 01100 11", sll    , R, Print_DBG_Message("sll"),R(rd) = src1 << src2);
  INSTPAT("0000000 ????? ????? 010 ????? 01100 11", slt    , R, Print_DBG_Message("slt"),R(rd) = ((sword_t)src1 < (sword_t)src2) ? 1 : 0);
  INSTPAT("0000000 ????? ????? 011 ????? 01100 11", sltu   , R, Print_DBG_Message("sltu"),R(rd) = (src1 < src2) ? 1 : 0);
  INSTPAT("0000001 ????? ????? 000 ????? 01100 11", mul    , R, Print_DBG_Message("mul"),R(rd) = src1 * src2);
  INSTPAT("0000001 ????? ????? 001 ????? 01100 11", mulh    , R, Print_DBG_Message("mulh"),R(rd) = ((int64_t)src1 * (int64_t)src2) >> 32);
  INSTPAT("0000001 ????? ????? 100 ????? 01100 11", div    , R, Print_DBG_Message("div"),R(rd) = (sword_t)src1 / (sword_t)src2);
  INSTPAT("0000001 ????? ????? 101 ????? 01100 11", divu    , R, Print_DBG_Message("divu"),R(rd) = src1 / src2);
  INSTPAT("0000001 ????? ????? 110 ????? 01100 11", rem    , R, Print_DBG_Message("rem"),R(rd) = src1 % src2);
  INSTPAT("0000001 ????? ????? 111 ????? 01100 11", remu    , R, Print_DBG_Message("remu"),R(rd) = (sword_t)src1 % (sword_t)src2);
  INSTPAT("??????? ????? ????? 000 ????? 11000 11", beq    , B, if (Print_DBG_Message("beq"),src1 == src2) s->dnpc += (sword_t)imm - 4);
  INSTPAT("??????? ????? ????? 001 ????? 11000 11", bne    , B, if (Print_DBG_Message("bne"),src1 != src2) s->dnpc += (sword_t)imm - 4);
  INSTPAT("??????? ????? ????? 100 ????? 11000 11", blt    , B, if (Print_DBG_Message("blt"),(sword_t)src1 < (sword_t)src2) s->dnpc += (sword_t)imm - 4);
  INSTPAT("??????? ????? ????? 101 ????? 11000 11", bge    , B, if (Print_DBG_Message("bge"),(sword_t)src1 >= (sword_t)src2) s->dnpc += (sword_t)imm - 4);
  INSTPAT("??????? ????? ????? 111 ????? 11000 11", bgeu   , B, if (Print_DBG_Message("bgeu"),src1 >= src2) s->dnpc += imm - 4);
  INSTPAT("0000000 00001 00000 000 00000 11100 11", ebreak , N, Print_DBG_Message("ebreak"),NEMUTRAP(s->pc, R(10))); // R(10) is $a0
  INSTPAT("??????? ????? ????? ??? ????? ????? ??", inv    , N, Print_DBG_Message("inv"),INV(s->pc));
  INSTPAT_END();

  R(0) = 0; // reset $zero to 0

  return 0;
}

void change_register_value(int regNO, word_t value){
  Log("Warn:You are doing a dangerous operation which may causing an unexpect rexsult.\n");
  R(regNO) = value;
}

int isa_exec_once(Decode *s) {
  s->isa.inst.val = inst_fetch(&s->snpc, 4);
  return decode_exec(s);
}
