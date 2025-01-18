ysyx_macro::mod_pub!(monitor_parser, nemu, mmu, disassembler, differtest);
ysyx_macro::mod_flat!(simulator, cmd, monitor);

use simulator::SimulatorOk as simOk;
use simulator::SimulatorError as simErr;
