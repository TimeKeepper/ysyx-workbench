#include <nvboard.h>
#include "VII_final.h"

void nvboard_bind_all_pins(VII_final* top) {
	nvboard_bind_pin( &top->io_vga_sync_vsync, 1, VGA_VSYNC);
	nvboard_bind_pin( &top->io_vga_sync_hsync, 1, VGA_HSYNC);
	nvboard_bind_pin( &top->io_vga_sync_valid, 1, VGA_BLANK_N);
	nvboard_bind_pin( &top->io_rgb, 12, VGA_R7, VGA_R6, VGA_R5, VGA_R4, VGA_G7, VGA_G6, VGA_G5, VGA_G4, VGA_B7, VGA_B6, VGA_B5, VGA_B4);
}
