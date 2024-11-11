PRJ = playground

BUILD_DIR = ./build

SRC_DIR = ${NPC_HOME}/src

SSRC_DIR = $(SRC_DIR)/ssrc

SSRC = $(shell find $(abspath $(SSRC_DIR)) -name "*.scala")
SSRC += $(shell find $(abspath ./) -name "*.scala")

TOPNAME ?= II_final
NXDC_FILES = $(abspath ./src/constr/top.nxdc)
V_FILE_GEN = $(BUILD_DIR)/$(TOPNAME).sv

all: default
default: verilog

$(V_FILE_GEN): $(SSRC)
	$(MAKE) verilog

verilog:
	# mkdir -p $(BUILD_DIR)
	$(MAKE) -C $(NPC_HOME) PLATFORM=BASYS verilog

clean:
	-rm -rf $(BUILD_DIR)

BIN = $(BUILD_DIR)/$(TOPNAME)

VERILATOR = verilator
VERILATOR_CFLAGS += -MMD --build -cc  \
				-O3 --x-assign fast --x-initial fast --noassert  \
				--trace 

CXXFLAGS += -I/usr/lib/llvm-14/include -std=c++14 -fno-exceptions
CXXFLAGS += -I$(INCLUDE) -I$(INC_PATH)

LDFLAGS += -lreadline -ldl

CSRC_DIR = $(abspath ./src/csrc)
VSRC_DIR = $(abspath ./src/vsrc)
OBJ_DIR  = $(abspath $(BUILD_DIR)/obj_dir)
INCLUDE  = $(abspath ./include)

SRC_AUTO_BIND = $(abspath $(CSRC_DIR)/auto_bind.cpp)
$(SRC_AUTO_BIND): $(NXDC_FILES)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

CSRC = $(shell find $(CSRC_DIR) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
CSRC += $(SRC_AUTO_BIND)
HSRC = $(shell find $(INCLUDE) -name "*.h" -or -name "*.hpp")

include $(NVBOARD_HOME)/scripts/nvboard.mk

$(BIN): $(CSRC) $(HSRC) $(V_FILE_GEN) $(NVBOARD_ARCHIVE)
	@rm -rf $(OBJ_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) --timescale "1ns/1ns" --no-timing\
		--top-module $(TOPNAME) $(V_FILE_GEN) $(CSRC) $(NVBOARD_ARCHIVE) \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN))

sim: $(BIN)
	$(BIN)

batch: $(BIN)
	$(BIN) -b

.PHONY: clean verilog syn perf SSRC
