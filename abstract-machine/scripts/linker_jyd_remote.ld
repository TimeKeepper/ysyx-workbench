ENTRY(_start)
PHDRS {text PT_LOAD; rodata PT_LOAD; data PT_LOAD; bss PT_LOAD; }

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */

  _sirom = ORIGIN(irom);
  _sdram = ORIGIN(dram);

  . = ORIGIN(irom);

  _begin = .;

  .text : {
    _stext = .;

    *(.text)
    *(.text*)
    
    . = ALIGN(4);

    _etext = .;
  } > irom AT> irom

  _srodata_load = LOADADDR(.rodata);

  .rodata : {
    _srodata = .;

    *(.rodata)
    *(.rodata*)
    *(.srodata)
    *(.srodata*)
    /* *(.srodata.mainargs) */

    . = ALIGN(4);

    _erodata = .;
  } > irom AT> irom

  _sdata_load = LOADADDR(.data);

  .data : {
    _sdata = .;

    *(.data)
    *(.data*)
    *(.data+)
    *(.sdata)
    *(.sdata*)
    *(.sdata+)

    . = ALIGN(4);

    _edata = .;
  } > dram AT> dram
  
  _sbss_load = LOADADDR(.bss);

  .bss : {
	  _sbss = .;

    *(.bss)
    *(.bss*)
    *(.sbss)
    *(.sbss*)
    *(.scommon)

    . = ALIGN(4);

    _ebss = .;

  } > dram AT> dram

  .stack (NOLOAD) : {

    _stack_top  = _essbl;

    *(.stack)

    . = ORIGIN(dram) + 0x10000;
    _stack_pointer = .;

  } > sram AT> sram

  .heap (NOLOAD) : {

    _sheap = .;

    *(.heap)
    *(.heap*)

    . = ORIGIN(dram) + LENGTH(dram);

    _eheap = .;
  } > dram AT> dram

  _end = .;
}
