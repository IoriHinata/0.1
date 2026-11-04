# DIGMA Z4 OS Lab — Android companion app

**Current app version: 0.2.1**

Android USB diagnostic and firmware-image inspection tool for research on the DIGMA Z4.

## Features
- USB Host discovery and explicit Android USB permission.
- Reads USB descriptors, interfaces and endpoints.
- Recognizes the exact identity in the supplied report: `10D7:1102`, manufacturer string `ACTIONS`, product string `HS USB FlashDisk`.
- Flags that identity as USB Mass Storage, not as a confirmed ADFU bootloader.
- Selects a local firmware/image file and calculates SHA-256.
- Reports file size, selected header signatures and image hash.
- Exports combined USB + image diagnostic report as TXT.
- Does not upload images or issue vendor-specific USB commands.

## Build and install
1. Open [GitHub Actions](https://github.com/IoriHinata/0.1/actions).
2. Open the latest successful **Android APK** run.
3. Download artifact **DIGMA-Z4-OS-Lab-v0.2.1**, extract it, then install `app-debug.apk`.
4. Connect the player with a data-capable OTG adapter and scan USB.

The APK is a debug build. Android may ask to allow installation from the browser or file manager.

## Hardware findings and compatibility

The official DIGMA product announcement identifies the DIGMA Z4 audio processor as **Actions Semiconductor ATJ2127**, and lists its 16 GB internal storage and 128×128 display:
https://digma.ru/news/kachestvo-i-shirokaya-funkcionalnost-mppleer-digma-z/

The supplied USB report, however, reports:
- VID:PID `10D7:1102`
- Manufacturer: `ACTIONS`
- Product: `HS USB FlashDisk`
- Interface class/subclass/protocol: `08/05/50` (USB Mass Storage / SCSI transparent / Bulk-Only Transport)

This is a meaningful mismatch. Public Actions USB ID records list Actions Semiconductor as VID `10D6`, not `10D7`. The well-known ATJ2127 reverse-engineering projects describe normal flash-disk mode as `10D6:1101` and ADFU mode as `10D6:10D6` (some other ADFU IDs also exist). They do **not** establish that `10D7:1102` is compatible with those commands.

References:
- Actions ATJ2127/ATJ2157 reverse-engineering toolkit and protocol notes: https://github.com/kilicdev/actions_flash
- Earlier ATJ2127 firmware dumper: https://github.com/ilyakurdyukov/actions_flash
- Actions ADFU device identifiers and USB handling: https://github.com/nfd/atj2127decrypt
- USB ID registry entry for VID 10D6: https://the-sz.com/products/usbid/index.php?v=0x10D6
- Digma Z4 manual: https://mcgrp.ru/manual/digma/z4
- Digma Z4 product specification and SoC: https://digma.ru/news/kachestvo-i-shirokaya-funkcionalnost-mppleer-digma-z/

A community post points to the vendor's Digma Z4 firmware directory:
https://www.ddixlab.com/public/digma/mp3/z4/
That directory should be checked for a package explicitly matching the exact Z4 hardware/revision. A Z4 BT package or a package for a different revision must not be assumed compatible.

## Why flashing is still disabled

The device is currently visible as a standard USB storage device. That gives no proof that the ADFU boot ROM is exposed. The known Actions reverse-engineering tools target specific USB IDs and hardware revisions. Their documentation warns that vendor flash drivers, memory addresses and flash-write sequences can permanently brick a player when mismatched.

Before enabling any write operation, this project needs:
1. A second USB report from the player in its actual firmware-update/boot mode, if the player exposes one.
2. Confirmation that the boot-mode VID/PID and interface descriptors match a documented Actions ADFU implementation.
3. Exact board/SoC confirmation for this unit and exact firmware package/revision.
4. A successful read-only backup of boot records and internal firmware, plus a verified restore path.
5. A custom OS image built for the confirmed ATJ2127 target and its actual flash layout.

The official DIGMA product page confirms ATJ2127 for the Z4 product family, but does not establish the flash geometry, bootloader protocol, RAM map or a Linux-compatible board support package for this individual unit. The 16 GB number is storage capacity, not RAM.

## Safety
Version 0.2.1 is still a diagnostics/image-inspection app, **not a flasher**. It intentionally does not send ADFU reboot commands, execute uploaded payloads, erase flash, or write memory. Do not use a generic `.img`/ `.bin` disk image: an ATJ2127 player requires a firmware/runtime built for its architecture and board, not a PC disk image.
