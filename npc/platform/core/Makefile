PRJ = playground

BUILD_DIR = ./build

SRC_DIR = ${NPC_HOME}/src

SSRC_DIR = $(SRC_DIR)/ssrc

SSRC = $(shell find $(abspath $(SSRC_DIR)) -name "*.scala")
SSRC += $(shell find $(abspath ./) -name "*.scala")
DESIGN_FILE ?= $(abspath ./ysyx_23060198.sv)
SDC_FILE ?= $(abspath ./ysyx_23060198.sdc)

all: default
default: verilog

$(shell mkdir -p $(BUILD_DIR))

clean:
	-rm -rf $(BUILD_DIR)

V_FILE_GEN = ./ysyx_23060198.sv

$(V_FILE_GEN): $(SSRC)
	$(MAKE) verilog

verilog: SSRC
	$(call git_commit, "generate verilog")
	# mkdir -p $(BUILD_DIR)
	mill -i $(PRJ).runMain Elaborate --target-dir ./
	sed -i -e 's/_\(aw\|ar\|w\|r\|b\)_\(\|bits_\)/_\1/g' $(V_FILE_GEN)
	sed -i '/firrtl_black_box_resource_files.f/, $$d' $(V_FILE_GEN)

syn: $(V_FILE_GEN)
	$(MAKE) -C $(YOSYS_HOME) sta DESIGN=ysyx_23060198 SDC_FILE=$(SDC_FILE) RTL_FILES=$(DESIGN_FILE) CLK_FREQ_MHZ=500 RESULT_DIR=$(abspath $(BUILD_DIR)/result)

.PHONY: clean verilog syn SSRC
