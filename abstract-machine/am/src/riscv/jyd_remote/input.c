#include <am.h>
#include "../riscv.h"
#include "klib.h"

#define SERIAL_PORT     (0x10000000)
#define SERIAL_LS       (SERIAL_PORT + 5) // line status
#define KEY_PORT     (0x10011000)

void __am_uart_rx(AM_UART_RX_T *rx) {
    if((inb(SERIAL_LS) & 0x1) == 0) {
        rx->data = 0xff;
        return;
    }

    rx->data = inb(SERIAL_PORT);
}

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  uint16_t keyscam_code = inw(KEY_PORT);
  uint8_t keycode_high = (keyscam_code >> 8) & 0xff;
  uint8_t keycode_low = keyscam_code & 0xff;

  static bool key_pressed[256] = {false};

  if(keycode_low == 0x00){
    kbd->keydown = false;
    kbd->keycode = 0;
    return;
  }

  if(keycode_high == 0xf0){
    key_pressed[keycode_low] = false;
  }else{
    key_pressed[keycode_low] = true;
  }

  kbd->keydown = false;
  kbd->keycode = 0;
  for(uint16_t i = 0; i <= 255; i++){
    if(key_pressed[i]){
      kbd->keydown = true;
      kbd->keycode = i;
      return;
    }
  }
  return;
}
