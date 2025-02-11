ysyx_macro::mod_flat!(api);
ysyx_macro::mod_flat!(dynamic_lib, nemu);

#[cfg(feature = "nemu")]
pub type Differtest = dynamic_lib::DiffertestDl;
#[cfg(feature = "npc")]
pub type Differtest = nemu::DiffertestNemu;

pub enum DiffertestDirection {
    ToDut = 0,
    ToRef = 1,
}

// case DiffertestDirection to bool
impl From<DiffertestDirection> for bool {
    fn from(direction: DiffertestDirection) -> Self {
        match direction {
            DiffertestDirection::ToDut => false,
            DiffertestDirection::ToRef => true,
        }
    }
}
