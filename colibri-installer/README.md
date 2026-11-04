# ColibriOS for DIGMA Z4 — Port & Installer Lab

This is a separate Android project inside the `0.1` repository. It is a **porting and read-only diagnostics workbench**, not a falsely advertised one-click flasher.

## Hardware facts and hard compatibility boundary

- DIGMA Z4 is reported by DIGMA as using Actions Semiconductor ATJ2127 and a 128×128 display.
- ATJ2127 is a MIPS32/MIPS16 hybrid SoC (see the ATJ2127 reverse-engineering references below).
- ColibriOS/KolibriOS upstream targets x86-compatible computers and is largely FASM/x86. A stock PC image cannot execute on ATJ2127.
- Therefore this project does not claim that an upstream ISO/IMG can be installed. The real goal is a **MIPS port of selected ColibriOS concepts/code**, plus a compatible boot chain and board-specific drivers. It may ultimately need a substantial kernel rewrite; it is not an ordinary image conversion.

## Current application scope (v0.1.0)

- Android USB Host enumeration and permission handling.
- Exact reporting of VID/PID, manufacturer/product, interfaces and endpoints.
- Identifies the known report identity `10D7:1102` as Mass Storage class, not confirmed Actions ADFU.
- Local image selection and read-only metadata inspection (size, SHA-256, signatures).
- Exportable diagnostic report.
- Explicit safety gate: no flash writes, no vendor SCSI commands, no guessed ADFU switching, no raw disk overwrite.

## Why install is disabled

The user's current report is `10D7:1102`, interface `08/05/50` (USB Mass Storage / SCSI transparent / Bulk-Only Transport). This does not match the documented Actions reference IDs `10D6:1101` (mass-storage) and `10D6:10D6` (ADFU) in the community `actions_flash` project. The manufacturer string "ACTIONS" is not proof of protocol compatibility. No verified DIGMA Z4 board revision, RAM map, NAND geometry, bootloader entry method, flash layout, recovery image, or ColibriOS MIPS kernel exists yet.

## Build

Open this directory as an Android project in Android Studio, or run:

```bash
gradle assembleDebug
```

The GitHub Actions workflow `.github/workflows/colibri-z4-android.yml` builds the APK and uploads it as an artifact.

## Research references

- DIGMA Z4 / ATJ2127 product announcement: https://digma.ru/news/kachestvo-i-shirokaya-funkcionalnost-mppleer-digma-z/
- ColibriOS official source: https://github.com/KolibriOS/kolibrios
- ColibriOS architecture statement (x86-compatible): https://github.com/KolibriOS/kolibrios
- ATJ2127 research and read-only dump tools: https://github.com/kilicdev/actions_flash
- ATJ2127 reverse engineering notes: https://github.com/kilicdev/actions_flash/blob/main/RE_NOTES.md
- Alternative implementation: https://github.com/ilyakurdyukov/actions_flash

## Milestones before any real installer can be enabled

1. Enter a documented boot/ADFU mode and identify the actual USB ID/protocol.
2. Read and preserve Boot ROM/SRAM and NAND boot records using a verified read-only method.
3. Determine board revision, RAM map, NAND geometry, partitions, checksums and recovery path.
4. Extract and analyze the exact stock firmware for this device.
5. Build a MIPS target bring-up (serial/UART or display heartbeat first), then memory, GPIO/buttons, display, storage and audio drivers.
6. Define a signed/validated update package and recovery strategy.
7. Only then implement writes, with verified device identity, backup validation, power checks and rollback.

**Never flash an image for another Actions player or a DIGMA Z4 BT.**
