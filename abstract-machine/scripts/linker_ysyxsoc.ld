ENTRY(_start)
PHDRS { text PT_LOAD; data PT_LOAD; }

MEMORY {
  mrom : ORIGIN = 0x20000000, LENGTH = 4K
  sram : ORIGIN = 0x0f000000, LENGTH = 8K
}
/* 
REGION_ALIAS("REGION_TEXT", mrom);    
REGION_ALIAS("REGION_RODATA", mrom);
REGION_ALIAS("REGION_DATA", sram);
REGION_ALIAS("REGION_BSS", mrom); 
*/

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */
  . = ORIGIN(mrom);

  _begin = .;

  .text : {
    _text = .;
  
    *(entry)
    *(.text*)
  
    etext = .;
  } > mrom AT> mrom : text

  .rodata : {
    _rodata = .;

    *(.rodata*)

    erodata = .;
  } > mrom AT> mrom
  

  .data : {
    _data = .;

    *(.data);

    edata = .;
  } > sram AT> mrom : data

  data_size =  SIZEOF(.data);
  data_load_start = LOADADDR(.data);
  
  .bss : {
	  _bss = .;

    *(.bss*)
    *(.sbss*)
    *(.scommon)

    ebss = .;
  } > mrom AT> mrom

  _heap_start = _data + data_size;

  _stack_top = _data + data_size;
  . = 0x0f002000;
  _stack_pointer = .;
  
  _end = .;
}
