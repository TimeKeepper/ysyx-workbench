use std::time::Instant;

use super::{AttchDirection, Device};

enum TimerReg {
    MtimeH,
    MtimeL,
}

impl From<u32> for TimerReg {
    fn from(val: u32) -> Self {
        match val {
            0x000 => TimerReg::MtimeH,
            0x004 => TimerReg::MtimeL,
            _ => panic!("Invalid TimerReg value: {:x}", val),
        }
    }
}

pub struct TimerFactory ;

impl TimerFactory {
    pub fn new(base: u32) -> Device {
        let start = Instant::now();

        let mut timer = Device::new(
            "Timer",
            (base)..(base + 0x1000),
            None,
        );

        timer.callback = Some(Box::new(move |dir, addr, _, mask| {
            if dir == AttchDirection::Read {
                match TimerReg::from(addr) {
                    TimerReg::MtimeH => {
                        mask.transform((start.elapsed().as_micros() >> 32) as u32)
                    }
                    TimerReg::MtimeL => {
                        mask.transform((start.elapsed().as_micros() & 0xFFFFFFFF) as u32)
                    }
                }
            } else {
                panic!("Timer write not supported");
            }
        }));

        timer
    }
}
