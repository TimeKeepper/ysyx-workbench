#ifndef __MAIN_H__
#define __MAIN_H__

#include <cstdint>
#include <stdio.h>
#include <nvboard.h>
#include <VysyxSoCFull.h>

#include <sdb/sdb.h>
#include <utils.h>

void nvboard_bind_all_pins(VysyxSoCFull* top);

#endif