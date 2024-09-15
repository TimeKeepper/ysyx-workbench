ENTRY(_start)
PHDRS {fsbl PT_LOAD; ssbl PT_LOAD; text PT_LOAD; test PT_LOAD; rodata PT_LOAD; data PT_LOAD; bss PT_LOAD; }

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */

  _ssdram = ORIGIN(sdram);

  . = ORIGIN(flash);

  _begin = .;

  .fsbl : {
    _sfsbl = .;
  
    *(entry)

    *(.text.FSBL)
    *(.text._trm_init)
    
    . = ALIGN(4);
  
    _efsbl = .;
  } > flash AT> flash : fsbl

  _sssbl_load = LOADADDR(.ssbl);

  .ssbl : {
    _sssbl = .;
  
    *(.text.SSBL)
  
    . = ALIGN(4);

    _essbl = .;
  } > sram AT> flash : ssbl

  _stext_load = LOADADDR(.text);

  .text : {
    _stext = .;

    *(.text)
    *(.text*)
    
    . = ALIGN(4);

    _etext = .;
  } > sdram AT> flash : text

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
  } > sdram AT> flash : rodata

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
  } > sdram AT> flash : data
  
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

  } > sdram AT> flash : bss

  .heap (NOLOAD) : {

    _sheap = .;

    *(.heap)
    *(.heap*)

    . = ORIGIN(sdram) + LENGTH(sdram);

    _eheap = .;
  } > sdram AT> sdram

  .stack (NOLOAD) : {

    _stack_top  = _essbl;

    *(.stack)

    . = ORIGIN(sram) + LENGTH(sram);
    _stack_pointer = .;

  } > sram AT> sram
  
  _end = .;
}
