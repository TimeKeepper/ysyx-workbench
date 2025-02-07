ysyx_macro::mod_pub!(nemu, npc, disassembler, differtest);
ysyx_macro::mod_flat!(simulator);

#[cfg(feature = "nemu")]
pub type Simulator = nemu::Simulator;
