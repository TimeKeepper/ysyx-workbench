Feature = llvm-sys/prefer-dynamic
Mainargs = --bin ./monitor/src/test/microbench-riscv32-nemu.bin --log -d ../nemu/tools/spike-diff/build/riscv32-spike-so

run :
	cargo run --release --bin core --features=$(Feature) -- $(Mainargs)

bench :
	cargo bench --features=$(Feature) --benches

bench_store :
	cargo bench --features=$(Feature) --bench decode_bench -- --save-baseline master

clean :
	cargo clean
