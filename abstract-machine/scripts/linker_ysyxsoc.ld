ENTRY(_start)
PHDRS { text PT_LOAD; data PT_LOAD; bss PT_LOAD; sdata PT_LOAD; }

MEMORY {
  mrom : ORIGIN = 0x20000000, LENGTH = 4K
  sram : ORIGIN = 0x0f000000, LENGTH = 8K
}

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */
  . = ORIGIN(mrom);

  _begin = .;

  .text : {
    _stext = .;
  
    *(entry)
    *(.text)
    *(.text*)
  
    _etext = .;
  } > mrom AT> mrom : text

  .rodata : {
    _srodata = .;

    *(.rodata)
    *(.rodata*)

    _erodata = .;
  } > mrom AT> mrom

  data_load_start = LOADADDR(.data);

  .data : {
    _sdata = .;

    *(.data)
    *(.data*)

    _edata = .;
  } > sram AT> mrom : data

  .sdata : {
    _ssdata = .;

    *(.sdata)
    *(.sdata*)

    _esdata = .;
  } > sram AT> mrom : sdata
  
  .bss : {
	  _sbss = .;

    *(.bss)
    *(.bss*)
    *(.sbss)
    *(.sbss*)
    *(.scommon)

    _ebss = .;
  } > sram AT> mrom : bss

  data_load_end = ADDR(.heap);

  data_load_size = _sheap - _sdata;

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
