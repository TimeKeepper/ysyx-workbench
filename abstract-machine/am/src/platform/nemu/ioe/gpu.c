#include <am.h>
#include <nemu.h>
#include <stdint.h>

#define SCREEN_HEIGHT inw(VGACTL_ADDR)
#define SCREEN_WIDTH inw(VGACTL_ADDR + 2)
#define SYNC_ADDR (VGACTL_ADDR + 4)

void __am_gpu_init() {
  // int i;
  // int w = SCREEN_WIDTH;  // TODO: get the correct width
  // int h = SCREEN_HEIGHT;  // TODO: get the correct height
  // uint32_t *fb = (uint32_t *)(uintptr_t)FB_ADDR;
  // for (i = 0; i < w * h; i ++) fb[i] = i;
  // outl(SYNC_ADDR, 1);
}

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

int printf(const char *fmt, ...);

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  if (!ctl->sync) return;

  size_t *data=ctl->pixels;
  for(int i = 0; i < ctl->h; i++){
    for(int j = 0; j < ctl->w; j++){
      printf("test\n");
      outl((((ctl->x + j) + (ctl->y + i) * SCREEN_WIDTH) * 4) + FB_ADDR, *(data++));
    }
  }
  
  outb(SYNC_ADDR, 1);
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
