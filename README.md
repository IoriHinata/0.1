# DIGMA Z4 OS Lab — Android companion app

**Current app version: 0.2.0**

Android tool for inspecting the DIGMA Z4 over USB and examining candidate OS/firmware image files.

## Features in 0.2.0
- USB Host discovery and explicit Android USB permission.
- Reads standard USB descriptors: VID/PID, device class, interfaces and endpoints.
- Selects a local image through Android's system file picker (no broad storage permission).
- Streams the selected file and calculates its complete SHA-256 digest.
- Reports file name, size, recognized header signature (ELF, ZIP, GZIP, Android sparse, PE, SquashFS or unknown) and digest.
- Exports a combined USB + image report as TXT.
- Runs image hashing off the UI thread.
- Does not upload the selected image or send it to the player.

## Install APK
1. Open the repository's Actions tab: https://github.com/IoriHinata/0.1/actions
2. Open the latest successful **Android APK** run.
3. Download artifact **DIGMA-Z4-OS-Lab-v0.2.0**, extract it, then install `app-debug.apk`.
4. Connect the player using a data-capable OTG adapter and scan USB.
5. Select an image to inspect and save the diagnostic report.

The APK is a debug build. Android may ask you to allow installation from the browser or file manager.

## Build
Requirements: JDK 17, Android SDK platform 35, Gradle 8.9.

```bash
gradle --no-daemon clean assembleDebug
```

GitHub Actions builds on pushes to `main` and manual workflow dispatch.

## Important limitation: flashing is intentionally disabled
Version 0.2.0 is an image-inspection and USB-diagnostics tool, **not yet a working flasher**. The app deliberately has no flash erase/write implementation and no undocumented vendor-specific USB commands.

A file extension, header signature or SHA-256 digest cannot prove that an image is compatible with a specific board. Before implementing a writer, the following must be established for the exact physical DIGMA Z4 revision:
1. Exact SoC marking, board revision and boot ROM/bootloader mode.
2. RAM size/map and flash type, geometry, partition layout and offsets.
3. USB bootloader protocol and its documented command/packet sequence.
4. A complete verified original firmware backup and a tested recovery/unbrick procedure.
5. A known-good image built for that exact hardware, with explicit target layout and integrity/signature checks.

A VID such as `0x10D6` alone does not identify the player or prove it is in a writable bootloader mode. The product's 16 GB capacity is storage, not RAM. A generic Linux or Windows disk image cannot be assumed to boot on this proprietary MP3 player; a small bare-metal runtime or RTOS may be a more realistic first target.

## Safety
Do not try random ADFU commands or write a generic `.img`/ `.bin` file. An incorrect erase/write can permanently disable the player. The disabled installation control is deliberate, not a temporary UI limitation.

## References
- DIGMA Z4 product information: https://merlion.com/press-center/reviews/5bfcf90f9b1e6
- Actions Semiconductor community reverse-engineering tools: https://github.com/ilyakurdyukov/actions_flash
- Related reverse-engineering notes: https://github.com/kilicdev/actions_flash
