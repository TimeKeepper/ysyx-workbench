use std::collections::HashMap;
use std::time::{Duration, Instant};

pub struct Profiler {
    measurements: HashMap<String, Duration>,
}

impl Profiler {
    pub fn new() -> Profiler {
        Profiler {
            measurements: HashMap::new(),
        }
    }

    pub fn start(&mut self, name: String) -> Instant {
        self.measurements.entry(name).or_insert(Duration::from_secs(0));
        Instant::now()
    }

    pub fn end(&mut self, name: String, start: Instant) {
        let duration = start.elapsed();
        *self.measurements.entry(name).or_insert(Duration::from_secs(0)) += duration;
    }

    pub fn print_report(&self) {
        for (name, duration) in &self.measurements {
            println!("{} took {:?}", name, duration);
        }
    }
}
