PLATFORM ?= ysyxsoc
PLATFORM_PATH = $(NPC_HOME)/platform/$(PLATFORM)
CORE_PATH = $(NPC_HOME)/platform/core

AGRS ?=
IMG ?=

default:
	$(MAKE) -C $(PLATFORM_PATH)

sim:
	$(MAKE) -C $(PLATFORM_PATH) sim AGGS=$(AGRS) IMG=$(IMG)

gdb:
	$(MAKE) -C $(PLATFORM_PATH) gdb AGGS=$(AGRS) IMG=$(IMG)

menuconfig:
	$(MAKE) -C $(PLATFORM_PATH) menuconfig

clean:
	$(MAKE) -C $(PLATFORM_PATH) clean

perf:
	$(MAKE) -C $(CORE_PATH) perf

.PHONY: default sim gdb menuconfig clean perf
