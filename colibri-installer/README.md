# ColibriOS for DIGMA Z4 — Port & Device Diagnostics Lab

Android project in the `0.1` repository. Current release target: **0.2.0**.

## What the Android app does

- Enumerates USB devices and displays VID/PID, product strings, interfaces and endpoints.
- Requests Android USB Host permission before opening a Mass Storage device.
- Runs a **read-only standard SCSI INQUIRY** over USB Mass Storage Bulk-Only Transport to read the device's vendor/product/revision identity.
- Selects a local firmware/OS file, streams it to compute SHA-256, size and first 64 bytes without loading the entire file into memory.
- Exports a diagnostic TXT report.
- Simulates installation readiness without pretending that firmware was installed.

The app does **not** send vendor-specific SCSI commands, switch ADFU mode, read raw NAND, or write flash. Those operations are deliberately not enabled until the exact Z4 USB protocol, board revision, NAND geometry and recovery method are established.

## Known hardware / architecture boundary

- DIGMA's product announcement identifies the DIGMA Z4 (not Z4 BT) with Actions Semiconductor ATJ2127 and a 128×128 display.
- ATJ2127 is a MIPS32/MIPS16 hybrid SoC.
- Upstream KolibriOS targets x86-compatible machines. A stock x86 image cannot run on ATJ2127.
- The actual target is a **new MIPS port / compatible small operating environment**, reusing suitable ideas or portable code from KolibriOS rather than trying to convert its x86 kernel mechanically.

## USB evidence currently available

The user's USB report showed `10D7:1102`, Mass Storage interface `08/05/50`, Bulk IN/OUT. The public `actions_flash` reference documents `10D6:1101` as normal Mass Storage and `10D6:10D6` as ADFU for reference devices. These are different IDs; therefore no automatic ADFU switch is assumed.

A successful SCSI INQUIRY confirms only that the standard Mass Storage command path works. It does **not** prove ADFU compatibility or prove the precise SoC identity.

## Build

Open `colibri-installer` in Android Studio, or run:

```bash
gradle assembleDebug
```

GitHub Actions workflow: `.github/workflows/colibri-z4-android.yml`. The APK is published as the workflow artifact `ColibriOS-DIGMA-Z4-Port-Lab` after a successful build.

## Research references

- DIGMA Z4 product announcement: https://digma.ru/news/kachestvo-i-shirokaya-funkcionalnost-mppleer-digma-z/
- KolibriOS upstream: https://github.com/KolibriOS/kolibrios
- ATJ2127 read-only diagnostics, ADFU research and NAND tooling: https://github.com/kilicdev/actions_flash
- ATJ2127 reverse-engineering notes: https://github.com/kilicdev/actions_flash/blob/main/RE_NOTES.md

## Hardware milestones

1. Confirm the real live USB identity and SCSI INQUIRY response from this exact Z4.
2. Find a repeatable, device-specific ADFU entry method and confirm the ADFU USB identity.
3. Read Boot ROM/SRAM and NAND metadata using a verified read-only method.
4. Acquire and validate the exact factory firmware and complete backup.
5. Identify board revision, memory map, NAND model/geometry, display controller and GPIO wiring.
6. Build a minimal MIPS payload and prove RAM execution before attempting any firmware integration.
7. Implement a MIPS kernel/runtime and Z4-specific display, input and storage drivers.
8. Design a recovery-capable update package. Flashing remains disabled until restore is demonstrated.

**Never flash an image for another Actions player or DIGMA Z4 BT.**
