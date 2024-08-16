ENTRY(_start)
PHDRS {boot PT_LOAD; text PT_LOAD; test PT_LOAD; rodata PT_LOAD; data PT_LOAD; bss PT_LOAD; }

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

  _stext_load = LOADADDR(.text);

  .text : {
    _stext = .;

    *(.text)
    *(.text*)

    _etext = .;
  } > psram AT> flash : text

  _srodata_load = LOADADDR(.rodata);

  .rodata : {
    _srodata = .;

    *(.rodata)
    *(.rodata*)
    *(.srodata)
    *(.srodata*)

    _erodata = .;
  } > psram AT> flash : rodata

  _sdata_load = LOADADDR(.data);

  .data : {
    _sdata = .;

    *(.data)
    *(.data*)
    *(.sdata)
    *(.sdata*)

    _edata = .;
  } > psram AT> flash : data
  
  _sbss_load = LOADADDR(.bss);

  .bss : {
	  _sbss = .;

    *(.bss)
    *(.bss*)
    *(.sbss)
    *(.sbss*)
    *(.scommon)

    _ebss = .;
  } > psram AT> flash : bss

  . = ALIGN(0x1000);

  .heap (NOLOAD) : {
    _sheap = .;

    *(.heap)
    *(.heap*)

    . = . + 0x200000;

    _eheap = .;
  } > psram AT> psram

  .stack (NOLOAD) : {

    _stack_top  = _eheap;

    *(.stack)

    . = ORIGIN(sram) + LENGTH(sram);
    _stack_pointer = .;

  } > sram AT> sram
  
  _end = .;
}
