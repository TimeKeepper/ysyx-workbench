Job = -j `nproc`

REF_Path_spike = ../nemu/tools/spike-diff/build/riscv32-spike-so
# REF_Path_nemu  = ../nemu/build/riscv32-nemu-interpreter-so
REF_Path_nemu  = /home/wenjiu/ysyx-workbench/nemu/build/riscv32-nemu-interpreter-so
REF_Path = $(REF_Path_nemu)

Feature = llvm-sys/prefer-dynamic
Mainargs = --bin ./monitor/src/test/microbench-riscv32-nemu.bin
Debugargs = $(Mainargs) --log -d $(REF_Path) --debug

run :
	cargo run $(Job) --release --bin core --features=$(Feature) -- $(Mainargs)

debug :
	cargo run $(Job) --bin core --features=$(Feature) -- $(Debugargs)

bench :
	cargo bench $(Job) --features=$(Feature) --benches

bench_store :
	cargo bench $(Job) --features=$(Feature) --bench decode_bench -- --save-baseline master

clean :
	cargo clean
