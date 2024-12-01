CXXFLAGS += -DPLATFORM_YSYXSOC

PRJ = playground

BUILD_DIR = ./build

SRC_DIR = ${NPC_HOME}/src
SOC_DIR = $(YSYXSOC_HOME)
SOC_VSRC_DIR = $(SOC_DIR)/perip
SOC_BUS = $(SOC_DIR)/build/ysyxSoCFull.v
SOC_UART_DIR = $(SOC_DIR)/perip/uart16550/rtl
SOC_SPI_DIR  = $(SOC_DIR)/perip/spi/rtl

VSRC_DIR = $(BUILD_DIR)
CSRC_DIR = $(SRC_DIR)/csrc
HEAD_DIR = $(abspath $(NPC_HOME)/include)
AUTOCONFIG = $(abspath ./include/generated)

DESIGN_FILE ?= $(abspath $(VSRC_DIR))/ysyx_23060198.sv
VSRC = $(shell find $(abspath $(VSRC_DIR)) -name "*.v" -or -name "*.sv")
VSRC += $(shell find $(abspath $(SOC_VSRC_DIR)) -name "*.v") # 将ysyxSOC/perip下所有v文件加入路径
VSRC += $(abspath $(SOC_BUS))
CSRC = $(shell find $(abspath $(CSRC_DIR)) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
HSRC = $(shell find $(abspath $(HEAD_DIR)) -name "*.h" -or -name "*.hpp")
HSRC += $(shell find $(abspath $(AUTOCONFIG)) -name "*.h" -or -name "*.hpp")

TOPNAME ?= ysyxSoCFull
NXDC_FILES = $(abspath $(SRC_DIR)/constr/top.nxdc)

VERILATOR = verilator
VERILATOR_CFLAGS += -MMD --build -cc  \
				-O3 --x-assign fast --x-initial fast --noassert  \
				--trace 

OBJ_DIR = $(BUILD_DIR)/obj_dir
BCSRC_DIR = $(BUILD_DIR)/csrc
BVSRC_DIR = $(BUILD_DIR)/vsrc
BIN = $(BUILD_DIR)/$(TOPNAME)

all: default
default: $(BIN)

$(shell mkdir -p $(BUILD_DIR))
$(shell mkdir -p $(BCSRC_DIR))
$(shell mkdir -p $(BVSRC_DIR))

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

V_FILE_GEN = $(VSRC_DIR)/ysyx_23060198.sv

-include $(NPC_HOME)/../Makefile

# constraint file
SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/csrc/auto_bind.cpp)
$(SRC_AUTO_BIND): $(NXDC_FILES)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

CSRC += $(SRC_AUTO_BIND)
	
# rules for NVBoard
include $(NVBOARD_HOME)/scripts/nvboard.mk

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
LDFLAGS += $(shell llvm-config --libs)
LDFLAGS += -lreadline -ldl -pie -lelf -lmuparser

$(BIN): $(CSRC) $(HSRC) $(VSRC) $(NVBOARD_ARCHIVE) $(DESIGN_FILE)
	@rm -rf $(OBJ_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) --timescale "1ns/1ns" --no-timing -j 0\
		-y $(SOC_SPI_DIR) -y $(SOC_UART_DIR) \
		--top-module $(TOPNAME) $(VSRC) $(CSRC) $(NVBOARD_ARCHIVE) \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN))

override ARGS ?= 
override IMG ?=
NPC_EXEC := $(BIN) $(ARGS) $(IMG)

sim: $(BIN) $(IMG)
	$(call git_commit, "sim YSYXSOC") # DO NOT REMOVE THIS LINE!!!
	$(NPC_EXEC)

gdb: $(BIN) $(IMG)
	$(call git_commit, "gdb YSYXSOC")
	gdb $(NPC_EXEC)
	
# Include rules for menuconfig
include ./scripts/config.mk

-include ../Makefile
