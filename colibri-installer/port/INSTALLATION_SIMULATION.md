# DIGMA Z4 installation dry-run

This document describes the exact workflow shown by the Android app's **Run installation simulation** action.

## Recorded target profile

- USB VID:PID: `10D7:1102`
- USB interface: Mass Storage, SCSI transparent, Bulk-Only Transport
- Reported player SoC: Actions Semiconductor ATJ2127
- CPU family: MIPS32/MIPS16
- Display: 128x128 (device specification; not used by the simulator)

The USB profile is replayed from the user's diagnostic report. The app does not claim that the player is currently connected when it displays this profile.

## Simulated installation sequence

| Stage | Operation | Dry-run result |
|---|---|---|
| 1 | Discover USB device | PASS — replay known VID:PID and interface |
| 2 | Identify target | PASS — load ATJ2127/MIPS target profile |
| 3 | Inspect image | HASHED — SHA-256 and first bytes only; no format claim |
| 4 | Enter ADFU | NOT RUN — no verified handshake |
| 5 | Read NAND geometry and layout | UNKNOWN — no device dump |
| 6 | Verify backup and recovery | NOT AVAILABLE |
| 7 | Check kernel architecture | BLOCKED — stock x86 KolibriOS does not execute on MIPS |
| 8 | Write flash and reboot | SKIPPED — no writes |

## Expected result

The simulator completes successfully as a simulation and stops the hypothetical installation at the CPU compatibility gate. This is the correct result for the currently known hardware profile. The Android app does not send SCSI vendor commands, upload payloads, erase NAND, or write firmware.

## Test cases

1. No physical USB device attached: simulation still runs using the saved diagnostic profile and labels it as replayed.
2. No image selected: image stage reports WAITING FOR IMAGE; remaining stages are still displayed.
3. Arbitrary file selected: app displays file size, SHA-256 and first 64 bytes, but does not mark it as a valid firmware image.
4. Export report: includes the simulation output and the read-only safety statement.
5. Scan USB: live USB enumeration remains separate from the replayed profile.

## Engineering continuation

The next real milestone is a read-only inspection of a genuine DIGMA Z4 factory firmware image or NAND dump. Parse its container and executable headers, identify the boot chain and memory map, and only then build a minimal MIPS test payload. Flash support remains out of scope until backup and recovery are independently verified.
