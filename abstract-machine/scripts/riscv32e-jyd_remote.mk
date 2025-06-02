include $(AM_HOME)/scripts/isa/riscv.mk
include $(AM_HOME)/scripts/platform/jyd_remote.mk
COMMON_CFLAGS += -march=rv32i_zicsr -mabi=ilp32  # overwrite
LDFLAGS       += -melf32lriscv                     # overwrite

AM_SRCS += riscv/jyd_remote/libgcc/div.S \
           riscv/jyd_remote/libgcc/muldi3.S \
           riscv/jyd_remote/libgcc/multi3.c \
           riscv/jyd_remote/libgcc/ashldi3.c \
           riscv/jyd_remote/libgcc/unused.c
