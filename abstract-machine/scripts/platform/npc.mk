AM_SRCS := riscv/npc/start.S \
           riscv/npc/trm.c \
           riscv/npc/ioe.c \
           riscv/npc/timer.c \
           riscv/npc/input.c \
           riscv/npc/cte.c \
           riscv/npc/trap.S \
           platform/dummy/vme.c \
           platform/dummy/mpe.c

CFLAGS    += -fdata-sections -ffunction-sections
LDFLAGS   += -T $(AM_HOME)/scripts/linker.ld \
						 --defsym=_pmem_start=0x20000000 --defsym=_entry_offset=0x0
LDFLAGS   += --gc-sections -e _start
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
	$(MAKE) -C $(NPC_HOME) trace ARGS="$(NPCFLAGS)" IMG=$(IMAGE).bin

batch: image
	$(MAKE) -C $(NPC_HOME) trace ARGS="$(NPC_BATCH_FLAG)" IMG=$(IMAGE).bin

trace: image
	$(MAKE) -C $(NPC_HOME) trace IMG=$(IMAGE).bin
