ysyx_macro::mod_pub!(nemu, npc, disassembler, differtest);
ysyx_macro::mod_flat!(simulator);

#[cfg(feature = "nemu")]
pub type Simulator = nemu::Simulator;

#[cfg(feature = "npc")]
pub type Simulator = npc::Simulator;