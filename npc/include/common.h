#include <cstdint>
#include <assert.h>
#include <cstdio>

#define DEFAULT_MSIZE 0x8000000
#define DEFAULT_MBASE 0x80000000

#if !defined(likely)
#define likely(cond)   __builtin_expect(cond, 1)
#define unlikely(cond) __builtin_expect(cond, 0)
#endif

#define TRACE

typedef uint32_t paddr_t;
typedef uint32_t word_t;

typedef word_t vaddr_t;