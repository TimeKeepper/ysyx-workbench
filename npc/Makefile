# PROJECT DIRECTORIES AND FILES DEFIENITIONS

PRJ = playground

BUILD_DIR = ./build

SRC_DIR = ./src

VSRC_DIR = $(SRC_DIR)/vsrc
SSRC_DIR = $(SRC_DIR)/ssrc
CSRC_DIR = $(SRC_DIR)/csrc
HEAD_DIR = $(abspath $(CSRC_DIR)/include)

SSRC = $(shell find $(abspath $(SSRC_DIR)) -name "*.scala")
DESIGN_FILE = $(abspath $(VSRC_DIR))/npc.sv
VSRC = DESIGN_FILE
VSRC += $(abspath $(VSRC_DIR))/top.v
CSRC = $(shell find $(abspath $(CSRC_DIR)) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
HSRC = $(shell find $(abspath $(HEAD_DIR)) -name "*.h" -or -name "*.hpp")

# yosys-sta DEFINITIONS

# DESIGN = top
# SDC_FILE = $(abspath $(VSRC_DIR))/$(DESIGN).sdc
# RTL_FILES = $(VSRC)
# export CLK_FREQ_MHZ = 500

# VERILATOR DIFIENITIONS

TOPNAME = top
NXDC_FILES = $(SRC_DIR)/constr/top.nxdc

VERILATOR = verilator
VERILATOR_CFLAGS += -MMD --build -cc  \
				-O3 --x-assign fast --x-initial fast --noassert  \
				--trace 

# FINAL TARGET DIFINITIONS

OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/$(TOPNAME)

all: default
default: $(BIN)

$(shell mkdir -p $(BUILD_DIR))

help:
	mill -i $(PRJ).runMain Elaborate --help

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

clean:
	-rm -rf $(BUILD_DIR)

.PHONY: test verilog help reformat checkformat clean

test:
	mill -i $(PRJ).test

verilog:
	$(call git_commit, "generate verilog")
	# mkdir -p $(BUILD_DIR)
	mill -i $(PRJ).runMain Elaborate --target-dir $(VSRC_DIR)

# NVBOARD DIFIENITIONS

# constraint file
SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
$(SRC_AUTO_BIND): $(NXDC_FILES)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@
	
# rules for NVBoard
include $(NVBOARD_HOME)/scripts/nvboard.mk

# VERILATOR DIFINITIONS

INCFLAGS =  $(addprefix -I, $(INC_PATH))
INCFLAGS += $(addprefix -I, $(HEAD_DIR))
CXXFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOPNAME)\""  
CXXFLAGS += -I/usr/lib/llvm-14/include -std=c++14   -fno-exceptions
CXXFLAGS += -g
LDFLAGS += $(shell llvm-config --libs)
LDFLAGS += -lreadline -ldl -pie -lelf

$(DESIGN_FILE): $(SSRC)
	$(MAKE) verilog

print:
	@echo $(INCFLAGS)

$(BIN): $(DESIGN_FILE) $(CSRC) $(HSRC) $(NVBOARD_ARCHIVE)
	@rm -rf $(OBJ_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(TOPNAME) $(VSRC) $(CSRC) $(NVBOARD_ARCHIVE) \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN))

override ARGS ?= 
override IMG ?=
NPC_EXEC := $(BIN) $(ARGS) $(IMG)

sim: $(BIN) $(IMG)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	$(NPC_EXEC)

trace:$(BIN) $(IMG)
	$(eval VERILATOR_CFLAGS += --trace)
	$(eval CXXFLAGS += -DTRACE)
	$(NPC_EXEC)

-include ../Makefile
