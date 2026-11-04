# DIGMA Z4 OS Lab — Android companion app

**Current app version: 0.1.1**

Android USB diagnostics and evidence-capture utility for investigating the DIGMA Z4 player. This app is a safe first-stage tool; it is not yet a firmware flasher and does not claim to install Linux/Windows.

## Features in 0.1.1
- USB Host device discovery on Android.
- Explicit Android USB permission request.
- Reads the descriptors exposed by Android: VID/PID, device class, interfaces and endpoint details.
- Shows manufacturer/product strings when the device supplies them.
- Exports a timestamped TXT diagnostic report using Android's document picker.
- Responsive, scrollable interface for phone screens.
- No network permission, no vendor-specific USB commands, no flash erase/write, and no bootloader modification.

## Install the APK
1. Open the repository's **Actions** tab.
2. Select the latest **Android APK** workflow run.
3. Download the `DIGMA-Z4-OS-Lab-v0.1.1` artifact ZIP.
4. Extract and install `app-debug.apk` on an Android device with USB Host/OTG support.
5. Connect the player using a data-capable OTG adapter, open the app, grant USB access, then save the diagnostic TXT report.

The APK is a debug build. Android may ask you to allow installation from the browser or file manager used to open it.

## Build
Requirements: JDK 17, Android SDK platform 35, Gradle 8.9.

```bash
gradle --no-daemon assembleDebug
```

GitHub Actions builds the debug APK on pushes to `main` and on manual workflow dispatch. The APK is uploaded as a workflow artifact.

## Important hardware facts and limitations
- The product is identified as DIGMA Z4; public product descriptions list 16 GB internal storage and a 128×128 display.
- 16 GB is storage capacity, **not RAM**. The available RAM and exact board/SoC revision must be measured or established from the actual unit.
- A USB VID alone does not prove the exact player model, processor revision, bootloader mode, or flash layout.
- The Android USB Host API exposes standard device/interface/endpoint descriptors. It does not automatically provide raw access to arbitrary internal flash.
- No compatible, verified bootable OS image is currently included. A generic Linux image cannot be assumed to boot on this proprietary player.

## Roadmap — gates before flashing
1. Capture reports from the player in normal USB mode and, only if the documented hardware procedure permits it, its boot/ADFU mode.
2. Identify the exact chip and board revision; establish RAM size/map and flash geometry from reliable evidence.
3. Determine the firmware container, boot chain, supported USB protocol and recovery method.
4. Obtain and verify a complete original firmware backup before any write capability is developed.
5. Prototype a tiny program/runtime for the actual CPU and memory limits. An RTOS-like menu or bare-metal application launcher may be more realistic than a desktop Linux distribution.
6. Implement flashing only after a tested recovery path exists, with image signature/hash checks, exact-device matching, battery/power checks, and explicit user confirmation.

## Safety
Do not select random firmware files or attempt undocumented ADFU commands. An incompatible payload or flash layout can permanently disable the player. The current app is deliberately read-only.

## References
- DIGMA Z4 product information: https://merlion.com/press-center/reviews/5bfcf90f9b1e6
- Community ATJ2127 reverse-engineering tools: https://github.com/ilyakurdyukov/actions_flash
- Related reverse-engineering notes: https://github.com/kilicdev/actions_flash
