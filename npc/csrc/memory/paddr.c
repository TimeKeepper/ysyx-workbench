#include <cassert>
#include <common.h>
#include <memory/paddr.h>
#include <memory/host.h>

#define PG_ALIGN __attribute((aligned(4096)))

static uint8_t pmem[DEFAULT_MSIZE] PG_ALIGN = {};

uint8_t* guest_to_host(paddr_t paddr) { return pmem + paddr - DEFAULT_MBASE; }
paddr_t host_to_guest(uint8_t *haddr) { return haddr - pmem + DEFAULT_MBASE; }

static word_t pmem_read(paddr_t addr, int len) {
    word_t ret = host_read(guest_to_host(addr), len);
    return ret;
}

static void pmem_write(paddr_t addr, int len, word_t data) {
    host_write(guest_to_host(addr), len, data);
}

static void out_of_bound(paddr_t addr) {
    printf("address =  0x%08x  is out of bound of pmem [ 0x%08x ,  0x%08x ]\n", 
    addr, PMEM_LEFT, PMEM_RIGHT);
    assert(0);
}

word_t paddr_read(paddr_t addr, int len) {
    if (likely(in_pmem(addr))) return pmem_read(addr, len);
    out_of_bound(addr);
    return 0;
}

void paddr_write(paddr_t addr, int len, word_t data) {
    if (likely(in_pmem(addr))) { pmem_write(addr, len, data); return; }
    out_of_bound(addr);
}

uint8_t* get_pmem(void) {
    return pmem;
}
