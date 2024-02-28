TOPNAME = top
NXDC_FILES = constr/top.nxdc
INC_PATH ?= $(abspath ./include)

VERILATOR = verilator
VERILATOR_CFLAGS += -MMD --build -cc  \
				-O3 --x-assign fast --x-initial fast --noassert  \
				--trace

BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/$(TOPNAME)

default: $(NPC_EXEC)

$(shell mkdir -p $(BUILD_DIR))

# constraint file
SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
$(SRC_AUTO_BIND): $(NXDC_FILES)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

# project source
VSRCS = $(shell find $(abspath ./vsrc) -name "*.v")
CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
CSRCS += $(SRC_AUTO_BIND)
HSRCS = $(shell find $(abspath ./include) -name "*.h" -or -name "*.hpp")

# rules for NVBoard
include $(NVBOARD_HOME)/scripts/nvboard.mk

# rules for verilator
INCFLAGS = $(addprefix -I, $(INC_PATH))
CXXFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOPNAME)\""

$(BIN): $(VSRCS) $(CSRCS) $(HSRCS) $(NVBOARD_ARCHIVE)
	@rm -rf $(OBJ_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(TOPNAME) $^ \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN))

override ARGS ?= 
override IMG ?=
NPC_EXEC := $(BIN) $(ARGS) $(IMG)

all: default

sim: $(BIN) $(IMG)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!	
	@echo "Write this Makefile by your self."
	$(NPC_EXEC)

trace:$(BIN) $(IMG)
	$(eval VERILATOR_CFLAGS += --trace)
	$(eval CXXFLAGS += -DTRACE)
	$(NPC_EXEC)

src:
	mkdir -p vsrc csrc
	rm -rf $(BUILD_DIR)
	touch vsrc/top.v
	touch csrc/top.cpp
	mkdir -p constr
	touch constr/top.nxdc
	code .

constr:
	mkdir -p constr
	touch constr/top.nxdc

clean:
	rm -rf $(BUILD_DIR)

.PHONY: default all clean run

include ../Makefile
