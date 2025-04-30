AM_SRCS := riscv/jyd_remote/start.S \
           riscv/jyd_remote/trm.c \
           riscv/jyd_remote/ioe.c \
           riscv/jyd_remote/timer.c \
           riscv/jyd_remote/input.c \
           riscv/jyd_remote/cte.c \
           riscv/jyd_remote/gpu.c \
           riscv/jyd_remote/trap.S \
           platform/dummy/vme.c \
           platform/dummy/mpe.c 

CFLAGS    += -fdata-sections -ffunction-sections 
LDFLAGS   += -T $(AM_HOME)/scripts/linker_jyd_remote.ld 
LDFLAGS   := -T $(AM_HOME)/scripts/linker_jyd_remote_mem.ld $(LDFLAGS) #多个linker script符号声明必须在之前的链接脚本完成,所以需要添加到最前面
LDFLAGS   += --gc-sections -e _start #--print-map
NPCFLAGS += -e $(IMAGE).elf
NPCFLAGS += -d $(NEMU_HOME)/build/riscv32-nemu-interpreter-so
NPC_BATCH_FLAG = $(NPCFLAGS)
NPC_BATCH_FLAG += -b
CFLAGS += -DMAINARGS=\"$(mainargs)\"
.PHONY: $(AM_HOME)/am/src/riscv/jyd_remote/trm.c

image: $(IMAGE).elf
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S --set-section-flags .bss=alloc,contents -O binary $(IMAGE).elf $(IMAGE).bin

run: image
	$(MAKE) -C $(NPC_HOME) sim ARGS="$(NPCFLAGS)" IMG=$(IMAGE).bin 

batch: image
	$(MAKE) -C $(NPC_HOME) sim ARGS="$(NPC_BATCH_FLAG)" IMG=$(IMAGE).bin 

gdb: image
	$(MAKE) -C $(NPC_HOME) gdb ARGS="$(NPC_BATCH_FLAG)" IMG=$(IMAGE).bin 
