# ColibriOS MIPS port — first kernel milestone

This begins a native MIPS32 port for the Actions ATJ2127 in DIGMA Z4. It is not
the upstream x86 kernel mechanically converted: x86 kernel code and FASM
applications cannot execute on this CPU. Portable concepts and userland
features must be reimplemented or ported at source level.

## Implemented
- MIPS32 little-endian entry stub.
- BSS initialization, private stack and C runtime entry.
- RAM-resident boot record (magic COLI, ABI 1) proving C entry.
- No guessed peripheral MMIO writes; kernel waits after publishing the record.
- Build emits ELF, raw binary, map and disassembly.

## Build
Install a MIPS little-endian cross toolchain, then run:
    make CROSS=mipsel-linux-gnu-

## Hardware boundary
The linker origin 0x9fc18000 is the ATJ2127 RAM payload address documented by
the public actions_flash reference. The 128 KiB linker window and stack placement
are initial build constraints, NOT a verified DIGMA Z4 RAM map. This is a
cross-compilation/RAM-execution artifact, not a firmware update package, NAND
image, or safe-to-flash bootloader.

Next: verify RAM bounds, CPU/cache state, timer/interrupt registers, LCD
controller and GPIO wiring from the exact Z4 firmware/board. Then implement
display, buttons, scheduler, memory manager, storage and audio drivers.
