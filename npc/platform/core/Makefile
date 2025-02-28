-include $(NPC_HOME)/../Makefile
PRJ = playground

BUILD_DIR = ./build

SRC_DIR = ${NPC_HOME}/src

SSRC_DIR = $(SRC_DIR)/ssrc

TARGET ?= ysyx_23060198

SSRC = $(shell find $(abspath $(SSRC_DIR)) -name "*.scala")
SSRC += $(shell find $(abspath ./) -name "*.scala")
DESIGN_FILE ?= $(abspath $(BUILD_DIR))/$(TARGET).sv
SDC_FILE ?= $(abspath ./ysyx_23060198.sdc)

V_FILE_GEN = $(BUILD_DIR)/$(TARGET).sv
PERFOMENSE_FILE = $(BUILD_DIR)/report.txt
RPT_FILE = $(BUILD_DIR)/result/ysyx_23060198.rpt
STAT_FILE = $(BUILD_DIR)/result/synth_stat.txt

all: default
default: verilog

$(shell mkdir -p $(BUILD_DIR))

clean:
	-rm -rf $(BUILD_DIR)

$(PERFOMENSE_FILE): $(SSRC)
	$(MAKE) -C $(NEMU_HOME)/../am-kernels/benchmarks/microbench ARCH=riscv32e-ysyxsoc mainargs=test batch

$(RPT_FILE) $(STAT_FILE): $(V_FILE_GEN)
	$(MAKE) syn

syn: $(V_FILE_GEN)
	$(MAKE) -C $(YOSYS_HOME) sta DESIGN=ysyx_23060198 SDC_FILE=$(SDC_FILE) RTL_FILES=$(DESIGN_FILE) CLK_FREQ_MHZ=500 RESULT_DIR=$(abspath $(BUILD_DIR)/result)

perf: $(RPT_FILE) $(STAT_FILE) $(PERFOMENSE_FILE)
	$(call git_commit, "$(shell python ./get_parameter.py)")
	@python3 ./perf.py

test: 
	$(MAKE) -C $(NEMU_HOME)/../rtthread_for_ysyx/bsp/abstract-machine ARCH=riscv32e-npc batch

.PHONY: clean verilog syn perf SSRC
