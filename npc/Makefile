PLATFORM ?= ysyxsoc
PLATFORM_PATH = $(NPC_HOME)/platform/$(PLATFORM)

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

.PHONY: default sim gdb menuconfig clean
