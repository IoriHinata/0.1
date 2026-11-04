# ATJ2127 ColibriOS Port — target-side work

## Important distinction

The upstream ColibriOS kernel is x86. This directory is the engineering workspace for a *new MIPS target port*, not a claim that upstream kernel sources already support MIPS. Do not copy an x86 `kernel.mnt`, ISO or floppy image to the player.

## Current code\nThe initial freestanding MIPS kernel is in `mips/`. It establishes an entry stub, C runtime and a RAM-resident diagnostic record. It deliberately avoids display/GPIO/MMIO writes until the exact board map is known. Build instructions and constraints are in `mips/README.md`.\n\n## Bring-up order

1. Confirm SoC stepping and exact board revision from a read-only NAND/firmware dump.
2. Identify exception vectors, reset path, RAM size/map, cache behaviour, clock tree and watchdog.
3. Establish a serial/UART or other harmless heartbeat without modifying NAND.
4. Implement a minimal MIPS32 reset/exception stub and linker map using only verified addresses.
5. Add a framebuffer/display driver for the 128×128 panel only after identifying controller, bus and timing.
6. Add GPIO/button input, storage read-only access, filesystem, audio and power management.
7. Decide which ColibriOS userland components can be ported. FASM x86 applications are not binary-compatible with MIPS; they require source-level ports or replacements.

## Why there is no linker script or flashable binary yet

No validated DIGMA Z4 memory map, RAM size, NAND geometry, boot ROM entry point, panel controller, board revision or recovery procedure has been established. Guessing any of these values would produce an unsafe binary. Add the linker script only after a verified memory map is committed here with evidence and provenance.

## Evidence log template

For every discovered value record:
- value and units;
- exact device/revision;
- method used to obtain it;
- raw report/dump hash;
- confidence (observed / inferred / unknown);
- source URL or capture filename.

Never promote an inferred address to a write target.
