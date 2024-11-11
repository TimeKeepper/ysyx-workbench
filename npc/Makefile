PRJ = npc

PLATFORM ?= ysyxsoc
PLATFORM_PATH = $(NPC_HOME)/platform/$(PLATFORM)
CORE_PATH = $(NPC_HOME)/platform/core
NPC_PATH = $(NPC_HOME)/platform/npc

AGRS ?=
IMG ?=

EXTRA_DEFILE ?= 

BUILD_DIR = $(PLATFORM_PATH)/build
VSRC_DIR = $(BUILD_DIR)
V_FILE_GEN ?= 

SRC_DIR = ${NPC_HOME}/src
SSRC_DIR = $(SRC_DIR)/ssrc

SSRC = $(shell find $(abspath $(SSRC_DIR)) -name "*.scala")

ifeq ($(PLATFORM), ysyxsoc)
	V_FILE_GEN = $(VSRC_DIR)/ysyx_23060198.sv
	# SSRC += $(shell find $(abspath $(YSYXSOC_HOME)) -name "*.scala")
	# SSRC += $(shell find $(abspath ./) -name "*.scala")
else
ifeq ($(PLATFORM), npc)
	V_FILE_GEN = $(VSRC_DIR)/top.sv
else
ifeq ($(PLATFORM), core)
	V_FILE_GEN = $(VSRC_DIR)/ysyx_23060198.sv
else
ifeq ($(PLATFORM), BASYS)
	V_FILE_GEN = $(shell find $(abspath $(VSRC_DIR)) -name "*.sv")
endif
endif
endif
endif

$(V_FILE_GEN): $(SSRC)
	$(MAKE) verilog

default:
	$(MAKE) -C $(PLATFORM_PATH)

verilog:
	$(call git_commit, "generate verilog")
	# mkdir -p $(BUILD_DIR)
	mill -i $(PRJ).runMain config.Elaborate$(PLATFORM) --target-dir $(VSRC_DIR)
	sed -i -e 's/_\(aw\|ar\|w\|r\|b\)_\(\|bits_\)/_\1/g' $(V_FILE_GEN)
	sed -i '/firrtl_black_box_resource_files.f/, $$d' $(V_FILE_GEN)

sim: $(V_FILE_GEN)
	$(MAKE) -C $(PLATFORM_PATH) sim AGGS=$(AGRS) IMG=$(IMG) EXTRA_DEFILE=$(EXTRA_DEFILE)

gdb:
	$(MAKE) -C $(PLATFORM_PATH) gdb AGGS=$(AGRS) IMG=$(IMG)

menuconfig:
	$(MAKE) -C $(PLATFORM_PATH) menuconfig

clean:
	$(MAKE) -C $(PLATFORM_PATH) clean

perf:
	$(MAKE) PLATFORM=core
	$(MAKE) -C $(CORE_PATH) perf

test:
	$(MAKE) PLATFORM=npc
	$(MAKE) -C $(CORE_PATH) test

.PHONY: default sim gdb menuconfig clean perf test verilog
