PRJ = playground

VERILOG_BUILD_DIR = ./vbuild

TOPNAME = top
NXDC_FILES = constr/top.nxdc
INC_PATH ?= $(abspath ./include)

VERILATOR = verilator
VERILATOR_CFLAGS += -MMD --build -cc  \
				-O3 --x-assign fast --x-initial fast --noassert  \
				--trace 

test:
	mill -i $(PRJ).test

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(VERILOG_BUILD_DIR)
	mill -i $(PRJ).runMain Elaborate --target-dir $(VERILOG_BUILD_DIR)

help:
	mill -i $(PRJ).runMain Elaborate --help

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

clean:
	-rm -rf $(VERILOG_BUILD_DIR)

.PHONY: test verilog help reformat checkformat clean

sim:
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@echo "Write this Makefile by yourself."

-include ../Makefile
