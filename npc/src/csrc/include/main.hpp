#ifndef __MAIN_H__
#define __MAIN_H__

#include <cstdint>
#include <stdio.h>
#include <stdlib.h>
#include <nvboard.h>

#ifdef PLATFORM_NPC
#include <Vtop.h>
#elif defined (PLATFORM_YSYXSOC)
#include <VysyxSoCFull.h>
#endif
#include <sdb/sdb.hpp>
#include <utils.hpp>

#endif