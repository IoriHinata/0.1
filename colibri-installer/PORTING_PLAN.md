# ColibriOS → DIGMA Z4 porting plan

## Scope

The goal is a device-specific MIPS port inspired by and derived from ColibriOS where licensing and architecture permit. It is not possible to install an unchanged x86 image on the ATJ2127. Upstream's x86 kernel and x86 assembly applications cannot execute natively on MIPS.

## Work packages

### A — Preserve and identify
- Obtain exact stock firmware package from official DIGMA archive.
- Capture full USB descriptors in normal and any documented update mode.
- Establish bootloader protocol by observation/source, not by guessing.
- Obtain redundant read-only NAND dumps and verify their hashes and consistency.
- Document a recovery procedure before any erase/write operation.

### B — CPU and boot
- Verify ATJ2127 core mode, reset vector, exception model, caches and memory map.
- Create MIPS cross-toolchain build.
- Implement a harmless RAM-only test payload.
- Verify load/execute/reset cycle without touching persistent storage.

### C — Minimal kernel
- Startup, exception vectors, interrupt controller and timer.
- UART diagnostics.
- Memory allocator and scheduler only after RAM map is known.
- 128×128 display and button input after board wiring/controller identification.

### D — Device functionality
- NAND controller + bad-block/ECC handling.
- Filesystem and safe read-only mount.
- Audio output/codec and power/battery reporting.
- Userland: port or reimplement selected ColibriOS utilities; x86 binaries are not portable.

### E — Installer
The Android app may only enable flashing after all gates are met:
- exact hardware identity allowlisted;
- target bootloader and protocol verified;
- package architecture and board revision match;
- image signature/hash and layout validated;
- complete verified backup exists;
- recovery path tested;
- power and connection remain stable.

Until then, the app remains a read-only diagnostic and image-inspection tool.
