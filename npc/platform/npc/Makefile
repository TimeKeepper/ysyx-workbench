CXXFLAGS += -DPLATFORM_NPC

PRJ = playground

BUILD_DIR = ./build

SRC_DIR = ${NPC_HOME}/src

VSRC_DIR = $(BUILD_DIR)
CSRC_DIR = $(SRC_DIR)/csrc
HEAD_DIR = $(abspath $(NPC_HOME)/include)
AUTOCONFIG = $(abspath ./include/generated)

DESIGN_FILE ?= $(abspath $(VSRC_DIR))/top.sv
VSRC = $(shell find $(abspath $(VSRC_DIR)) -name "*.v" -or -name "*.sv")
CSRC = $(shell find $(abspath $(CSRC_DIR)) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
HSRC = $(shell find $(abspath $(HEAD_DIR)) -name "*.h" -or -name "*.hpp")
HSRC += $(shell find $(abspath $(AUTOCONFIG)) -name "*.h" -or -name "*.hpp")

TOPNAME ?= top

VERILATOR = verilator
VERILATOR_CFLAGS += -MMD --build -cc  \
				-O3 --x-assign fast --x-initial fast --noassert  \
				--trace --trace-fst 

OBJ_DIR = $(BUILD_DIR)/obj_dir
SO_OBJ_DIR = $(BUILD_DIR)/so_obj_dir
BVSRC_DIR = $(BUILD_DIR)/vsrc
BIN = $(BUILD_DIR)/$(TOPNAME)

default: $(BIN)
all: default

$(shell mkdir -p $(BUILD_DIR))
$(shell mkdir -p $(BVSRC_DIR))

clean:
	-rm -rf $(BUILD_DIR)

.PHONY: all default clean

-include $(NPC_HOME)/../Makefile

verilog:
	$(call git_commit, "generate verilog")
	# mkdir -p $(BUILD_DIR)
	mill -i $(PRJ).runMain Elaborate --target-dir $(VSRC_DIR)
	sed -i -e 's/_\(aw\|ar\|w\|r\|b\)_\(\|bits_\)/_\1/g' $(DESIGN_FILE)
	sed -i '/firrtl_black_box_resource_files.f/, $$d' $(DESIGN_FILE)

# VERILATOR DIFINITIONS

INCFLAGS =  $(addprefix -I, $(INC_PATH))
INCFLAGS += $(addprefix -I, $(HEAD_DIR))
INCFLAGS += $(addprefix -I, $(AUTOCONFIG))
CXXFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOPNAME)\""  
CXXFLAGS += -I/usr/lib/llvm-14/include -std=c++14   -fno-exceptions
ifdef EXTRA_DEFILE
CXXFLAGS += $(EXTRA_DEFILE)
endif
CXXFLAGS += -g -DTRACE
LDFLAGS += $(shell llvm-config-14 --libs)
LDFLAGS += -lreadline -ldl -pie -lelf

include $(NVBOARD_HOME)/scripts/nvboard.mk

$(BIN): $(CSRC) $(HSRC) $(DESIGN_FILE) $(NVBOARD_ARCHIVE) $(DESIGN_FILE)
	@rm -rf $(OBJ_DIR)
	ccache $(VERILATOR) $(VERILATOR_CFLAGS) --timescale "1ns/1ns" --no-timing\
		--top-module $(TOPNAME) $(DESIGN_FILE) $(CSRC) $(NVBOARD_ARCHIVE) \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN))

override ARGS ?= 
override IMG ?=
NPC_EXEC := $(BIN) $(ARGS) $(IMG)

sim: $(BIN) $(IMG)
	$(call git_commit, "sim npc") # DO NOT REMOVE THIS LINE!!!
	$(NPC_EXEC)

gdb: $(BIN) $(IMG)
	$(call git_commit, "gdb npc")
	gdb $(NPC_EXEC)

shared: $(DESIGN_FILE)
	$(VERILATOR) \
		--cc -sv --build $(DESIGN_FILE) \
		../lib.cpp --Mdir $(SO_OBJ_DIR) \
		-CFLAGS "-shared -fPIC" --lib-create "top"
	
# Include rules for menuconfig
include ./scripts/config.mk

-include ../Makefile
