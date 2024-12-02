#ifndef __COMMON_HPP__
#define __COMMON_HPP__

#include <cstdint>
#include <autoconf.h>
#include <iostream>

#define ARRLEN(arr) (int)(sizeof(arr) / sizeof(arr[0]))
#define BITMASK(bits) ((1ull << (bits)) - 1)
#define BITS(x, hi, lo) (((x) >> (lo)) & BITMASK((hi) - (lo) + 1)) // similar to x[hi:lo] in verilog
#define SEXT(x, len) ({ struct { int64_t n : len; } __x = { .n = x }; (uint64_t)__x.n; })

#if !defined(likely)
#define likely(cond)   __builtin_expect(cond, 1)
#define unlikely(cond) __builtin_expect(cond, 0)
#endif

typedef uint32_t paddr_t;
typedef uint32_t word_t;

typedef word_t vaddr_t;

#endif
