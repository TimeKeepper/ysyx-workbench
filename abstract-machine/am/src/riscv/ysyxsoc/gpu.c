#include <am.h>
#include "../riscv.h"

#define SCREEN_HEIGHT 480
#define SCREEN_WIDTH 640
#define FB_ADDR (0x21000000)

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  uint16_t w = SCREEN_WIDTH;
  uint16_t h = SCREEN_HEIGHT;
  uint32_t vmemsz = w * h * sizeof(uint32_t);
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = w, .height = h,
    .vmemsz = vmemsz
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  size_t *data=ctl->pixels;
  for(int i = 0; i < ctl->h; i++){
    for(int j = 0; j < ctl->w; j++){
      outl((((ctl->x + j) + (ctl->y + i) * SCREEN_WIDTH) * 4) + FB_ADDR, *(data++));
    }
  }
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
