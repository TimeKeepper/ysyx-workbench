AM_SRCS := riscv/ysyxsoc/start.S \
           riscv/ysyxsoc/trm.c \
           riscv/ysyxsoc/ioe.c \
           riscv/ysyxsoc/timer.c \
           riscv/ysyxsoc/input.c \
           riscv/ysyxsoc/cte.c \
           riscv/ysyxsoc/gpu.c \
           riscv/ysyxsoc/trap.S \
           platform/dummy/vme.c \
           platform/dummy/mpe.c 

CFLAGS    += -fdata-sections -ffunction-sections 
LDFLAGS   += -T $(AM_HOME)/scripts/linker_ysyxsoc.ld 
LDFLAGS   := -T $(AM_HOME)/scripts/linker_ysyxsoc_mem.ld $(LDFLAGS) #多个linker script符号声明必须在之前的链接脚本完成,所以需要添加到最前面
LDFLAGS   += --gc-sections -e _start #--print-map
NPCFLAGS += -e $(IMAGE).elf
NPCFLAGS += -d $(NEMU_HOME)/build/riscv32-nemu-interpreter-so
NPC_BATCH_FLAG = $(NPCFLAGS)
NPC_BATCH_FLAG += -b
CFLAGS += -DMAINARGS=\"$(mainargs)\"
.PHONY: $(AM_HOME)/am/src/riscv/ysyxsoc/trm.c

image: $(IMAGE).elf
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S --set-section-flags .bss=alloc,contents -O binary $(IMAGE).elf $(IMAGE).bin

run: image
	# $(MAKE) -C $(NPC_HOME) sim ARGS="$(NPCFLAGS)" IMG=$(IMAGE).bin TOPNAME=ysyxSoCFull PLATFORM=ysyxsoc EXTRA_DEFILE=-DNAME=$(NAME)
	@$(MAKE) -C $(REMU_HOME) run Platform=$(ISA)-nzea-ysyxsoc Binfile=$(IMAGE).bin

debug: image
	@$(MAKE) -C $(REMU_HOME) debug Platform=$(ISA)-nzea-ysyxsoc Binfile=$(IMAGE).bin

batch: image
	$(MAKE) -C $(NPC_HOME) sim ARGS="$(NPC_BATCH_FLAG)" IMG=$(IMAGE).bin TOPNAME=ysyxSoCFull PLATFORM=ysyxsoc EXTRA_DEFILE=-DNAME=$(NAME)

gdb: image
	$(MAKE) -C $(NPC_HOME) gdb ARGS="$(NPC_BATCH_FLAG)" IMG=$(IMAGE).bin TOPNAME=ysyxSoCFull PLATFORM=ysyxsoc EXTRA_DEFILE=-DNAME=$(NAME)
