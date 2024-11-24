#include <nvboard.h>
#include "Vtop.h"

void nvboard_bind_all_pins(Vtop* top) {
	nvboard_bind_pin( &top->io_led1, 1, LD0);
	nvboard_bind_pin( &top->io_led2, 1, LD1);
}
