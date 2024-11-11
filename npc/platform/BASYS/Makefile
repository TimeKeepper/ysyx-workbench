-include $(NPC_HOME)/../Makefile
PRJ = playground

BUILD_DIR = ./build

SRC_DIR = ${NPC_HOME}/src

SSRC_DIR = $(SRC_DIR)/ssrc

SSRC = $(shell find $(abspath $(SSRC_DIR)) -name "*.scala")
SSRC += $(shell find $(abspath ./) -name "*.scala")

PERFOMENSE_FILE = $(BUILD_DIR)/report.txt
STAT_FILE = $(BUILD_DIR)/result/synth_stat.txt

all: default
default: verilog

$(shell mkdir -p $(BUILD_DIR))

clean:
	-rm -rf $(BUILD_DIR)

$(PERFOMENSE_FILE): $(SSRC)
	$(MAKE) -C $(NEMU_HOME)/../am-kernels/benchmarks/microbench ARCH=riscv32e-ysyxsoc mainargs=test batch

test: 
	$(MAKE) -C $(NEMU_HOME)/../rtthread_for_ysyx/bsp/abstract-machine ARCH=riscv32e-npc batch

.PHONY: clean verilog syn perf SSRC
