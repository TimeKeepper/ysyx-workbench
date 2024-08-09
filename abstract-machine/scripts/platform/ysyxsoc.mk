AM_SRCS := riscv/ysyxsoc/start.c \
           riscv/ysyxsoc/trm.c \
           riscv/ysyxsoc/ioe.c \
           riscv/ysyxsoc/timer.c \
           riscv/ysyxsoc/input.c \
           riscv/ysyxsoc/cte.c \
           riscv/ysyxsoc/trap.S \
           platform/dummy/vme.c \
           platform/dummy/mpe.c

CFLAGS    += -fdata-sections -ffunction-sections
LDFLAGS   += -T $(AM_HOME)/scripts/linker_ysyxsoc.ld 
LDFLAGS   += --gc-sections -e _start # --print-map
NPCFLAGS += -e $(IMAGE).elf
NPCFLAGS += -d /home/wen-jiu/my_ysyx_project/ysyx-workbench/nemu/build/riscv32-nemu-interpreter-so
NPC_BATCH_FLAG = $(NPCFLAGS)
NPC_BATCH_FLAG += -b
CFLAGS += -DMAINARGS=\"$(mainargs)\"
.PHONY: $(AM_HOME)/am/src/riscv/npc/trm.c

image: $(IMAGE).elf
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S --set-section-flags .bss=alloc,contents -O binary $(IMAGE).elf $(IMAGE).bin

run: image
	$(MAKE) -C $(NPC_HOME) trace ARGS="$(NPCFLAGS)" IMG=$(IMAGE).bin TOPNAME=ysyxSoCFull

batch: image
	$(MAKE) -C $(NPC_HOME) trace ARGS="$(NPC_BATCH_FLAG)" IMG=$(IMAGE).bin

trace: image
	$(MAKE) -C $(NPC_HOME) trace IMG=$(IMAGE).bin
