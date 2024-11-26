#ifndef __MAIN_H__
#define __MAIN_H__

#include <stdlib.h>
#include <nvboard.h>

#ifdef PLATFORM_NPC
#include <Vtop.h>
#elif defined (PLATFORM_YSYXSOC)
#include <VysyxSoCFull.h>
#endif
#include <utils.hpp>

#endif