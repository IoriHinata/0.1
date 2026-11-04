# DIGMA Z4 OS Flasher — v0.1.0

Android USB diagnostic app for the DIGMA Z4 (Actions Semiconductor ATJ2127). The first milestone is deliberately read-only.

## What v0.1 does
- Enumerates USB devices through Android USB Host API.
- Recognizes the commonly documented Actions IDs `10D6:1101` (normal flash-disk mode) and `10D6:10D6` (ADFU/boot mode candidate).
- Requests Android USB permission and displays VID:PID, interfaces and endpoint descriptors.
- Does **not** send undocumented vendor commands, write flash, erase NAND, or install an OS.

## Build
Open this project in Android Studio (JDK 17, Android SDK 35), or run:

```bash
gradle assembleDebug
```

GitHub Actions builds `app-debug.apk` on every push to `main` and on manual dispatch. Download it from the workflow run's Artifacts section.

## Hardware connection
Use an Android phone/tablet with USB Host (OTG) support and a data-capable OTG adapter. A charge-only cable will not work. Connect the player directly; do not connect it through a hub during initial diagnosis.

## Important technical status
DIGMA Z4 is documented with the Actions ATJ2127 SoC, 16 GB internal storage and a 128×128 display. 16 GB is storage, not RAM. Public ATJ2127 tools describe a MIPS-based chip and a 256 KiB RAM readout, but that figure and memory map must be verified against this exact player before any payload is run.

The public `actions_flash` project documents a read/flash workflow but explicitly warns that parts are unfinished and that loading the wrong payload can leave a player stuck in ADFU mode. Its flash-read path requires device/firmware-specific support. Therefore v0.1 only identifies the USB device. No OS image is bundled because there is currently no verified bootable Linux/RTOS image and no confirmed boot chain for this exact Z4 revision.

## Next milestones
1. Capture USB descriptors from the actual Z4 in normal and boot/ADFU modes.
2. Add a strictly read-only RAM/ROM identification path for the exact chip revision.
3. Determine flash geometry, RAM map, boot process, firmware container and recovery method; save and verify a complete original backup.
4. Only then prototype a tiny MIPS application/runtime or RTOS-like shell. A full desktop Linux distribution is unlikely to fit the platform's constraints.
5. Add flashing only after a tested recovery path exists; require checksum verification, explicit confirmation and power/battery checks.

## References
- DIGMA Z4 specifications: https://merlion.com/press-center/reviews/5bfcf90f9b1e6
- ATJ2127 USB/ADFU tools and warnings: https://github.com/ilyakurdyukov/actions_flash
- ATJ2127 reverse-engineering notes: https://github.com/kilicdev/actions_flash
