#ifndef __MAIN_H__
#define __MAIN_H__

#include <cstdint>
#include <stdio.h>
#include <nvboard.h>
#include <Vtop.h>
#include "verilated.h"
#include "verilated_vcd_c.h"
#include "Vtop__Dpi.h"

#include <sdb/sdb.h>
#include <cpu/cpu.h>
#include <utils.h>

void nvboard_bind_all_pins(Vtop* top);

#endif