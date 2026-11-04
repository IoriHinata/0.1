OUTPUT_FORMAT("elf32-tradlittlemips")
OUTPUT_ARCH(mips)
ENTRY(_start)
MEMORY { RAM (rwx) : ORIGIN = 0x9fc18000, LENGTH = 0x00020000 }
SECTIONS {
  . = ORIGIN(RAM);
  .text : ALIGN(16) { KEEP(*(.text.boot)) *(.text .text.*) } > RAM
  .rodata : ALIGN(16) { *(.rodata .rodata.*) } > RAM
  .data : ALIGN(16) { *(.data .data.*) } > RAM
  .bss (NOLOAD) : ALIGN(16) {
    __bss_start = .; *(.bss .bss.* COMMON) __bss_end = .;
  } > RAM
  .noinit (NOLOAD) : ALIGN(16) { *(.noinit) } > RAM
  .stack (NOLOAD) : ALIGN(16) { . += 4096; __stack_top = .; } > RAM
  /DISCARD/ : { *(.comment) *(.note*) *(.eh_frame*) }
  ASSERT(. <= ORIGIN(RAM) + LENGTH(RAM), "MIPS kernel exceeds reserved RAM window")
}
