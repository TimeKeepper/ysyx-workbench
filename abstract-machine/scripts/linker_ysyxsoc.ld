ENTRY(_start)
PHDRS {boot PT_LOAD; text PT_LOAD; test PT_LOAD; rodata PT_LOAD; srodata PT_LOAD; data PT_LOAD; bss PT_LOAD; sdata PT_LOAD; }

MEMORY {
  sram : ORIGIN = 0x0f000000, LENGTH = 8K
  mrom : ORIGIN = 0x20000000, LENGTH = 4K
  flash : ORIGIN = 0x30000000, LENGTH = 16M
}

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */
  . = ORIGIN(flash);

  _begin = .;

  .boot : {
    _sboot = .;
  
    *(entry)

    *(.text.boot_loader)
    *(.text._trm_init)
  
    _eboot = .;
  } > flash AT> flash : boot

  data_load_start = LOADADDR(.text);

  .text : {
    _stext = .;

    *(.text)
    *(.text*)

    _etext = .;
  } > sram AT> flash : text

  .rodata : {
    _srodata = .;

    *(.rodata)
    *(.rodata*)

    _erodata = .;
  } > sram AT> flash : rodata

  .srodata : {
    _ssrodata = .;

    *(.srodata)
    *(.srodata*)

    _esrodata = .;
  } > sram AT> flash : srodata

  .data : {
    _sdata = .;

    *(.data)
    *(.data*)

    _edata = .;
  } > sram AT> flash : data

  .sdata : {
    _ssdata = .;

    *(.sdata)
    *(.sdata*)

    _esdata = .;
  } > sram AT> flash : sdata
  
  .bss : {
	  _sbss = .;

    *(.bss)
    *(.bss*)
    *(.sbss)
    *(.sbss*)
    *(.scommon)

    _ebss = .;
  } > sram AT> flash : bss

  data_load_end = ADDR(.heap);

  data_load_size = _sheap - _stext;

  .heap (NOLOAD) : {
    _sheap = .;

    *(.heap)
    *(.heap*)

    . = . + 0x1000; /* align to 4K */

    _eheap = .;
  } > sram AT> sram

  .stack (NOLOAD) : {

    _stack_top  = _eheap;

    *(.stack)

    . = ORIGIN(sram) + LENGTH(sram);
    _stack_pointer = .;

  } > sram AT> sram
  
  _end = .;
}
