Job = -j `nproc`

REF_Path_spike = ../nemu/tools/spike-diff/build/riscv32-spike-so
# REF_Path_nemu  = ../nemu/build/riscv32-nemu-interpreter-so
REF_Path_nemu  = /home/wenjiu/ysyx-workbench/nemu/build/riscv32-nemu-interpreter-so
REF_Path = $(REF_Path_nemu)

Binfile = ./monitor/src/test/microbench-riscv32-nemu.bin

ExtraArgs ?=

Mainargs = --bin $(Binfile) $(ExtraArgs)
Debugargs = $(Mainargs) -d $(REF_Path) --debug

PLATFORM ?= nemu

Feature = $(PLATFORM),riscv32

run :
	cargo run $(Job) --features $(Feature) --release --bin core -- $(Mainargs)-d $(REF_Path)

debug :
	RUST_BACKTRACE=full cargo run $(Job) --features $(Feature),differtest,log --bin core -- $(Debugargs)

bench :
	cargo bench $(Job) --benches

bench_store :
	cargo bench $(Job) --bench decode_bench -- --save-baseline master

help:
	cargo run $(Job) --release --bin core -- --help

clean :
	cargo clean

format :
	cargo fmt

.PHONY : run debug bench bench_store help clean format
