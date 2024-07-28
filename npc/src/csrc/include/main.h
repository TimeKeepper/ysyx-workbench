#ifndef __MAIN_H__
#define __MAIN_H__

#include <cstdint>
#include <stdio.h>
#include <nvboard.h>

#ifdef DEFINE_NPC
#include <Vtop.h>
#else
#include <VysyxSoCFull.h>
#endif
#include <sdb/sdb.h>
#include <utils.h>

void nvboard_bind_all_pins(TOP_NAME* top);

#endif