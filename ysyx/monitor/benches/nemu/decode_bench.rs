use std::hint::black_box;
use criterion::{criterion_group, criterion_main, Criterion};

use monitor::nemu::decode::RvInstParser as decoder;

use rand::Rng;

fn bench(c: &mut Criterion) {
    let decoder = decoder::new();

    let mut rng = rand::thread_rng();
    let batch_size: u64 = 1000;

    let mut group = c.benchmark_group("decode");

    group.throughput(criterion::Throughput::Elements(batch_size));

    group.bench_function("decode", |b| {
        b.iter_batched(|| {
            let mut test_data = vec![0; batch_size as usize];
            rng.fill(test_data.as_mut_slice());
            test_data
        }, |key| {
            for i in 0..batch_size {
                let _ = black_box(decoder.parse(key[i as usize]));
            }
        }, criterion::BatchSize::NumBatches(batch_size));
    });
}

criterion_group!(benches, bench);
criterion_main!(benches);