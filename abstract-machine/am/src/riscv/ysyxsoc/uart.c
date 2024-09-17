#include <am.h>
#include "../riscv.h"

#define SERIAL_PORT     (0x10000000)
#define SERIAL_LS       (SERIAL_PORT + 5) // line status

void __am_uart_rx(AM_UART_RX_T *rx) {
    if((inb(SERIAL_LS) & 0x1) == 0) {
        rx->data = 0xff;
        return;
    }

    rx->data = inb(SERIAL_PORT);
}
