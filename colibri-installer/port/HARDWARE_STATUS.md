# DIGMA Z4 hardware evidence

| Item | Current evidence | Status |
|---|---|---|
| SoC | Actions Semiconductor ATJ2127 per DIGMA announcement | Reported by manufacturer |
| CPU family | MIPS32/MIPS16 hybrid in ATJ2127 reverse-engineering work | External technical reference |
| Display | 128×128 per DIGMA announcement | Manufacturer specification |
| USB normal mode | VID:PID 10D7:1102, interface 08/05/50 | Directly observed twice |
| USB boot/ADFU mode | Not observed | Unknown |
| RAM capacity/map | Not established | Unknown |
| NAND model/geometry | Not established | Unknown |
| Board revision | Not established | Unknown |
| Boot ROM entry point for this board | Not established | Unknown |
| Display controller and wiring | Not established | Unknown |
| Verified factory recovery image | Not yet acquired/validated | Unknown |
| ColibriOS MIPS kernel | Does not exist in this project yet | Not implemented |

## Existing USB observation

The device appears as a standard USB Mass Storage interface:
- class 08 = Mass Storage
- subclass 05 = SCSI transparent
- protocol 50 = Bulk-Only Transport
- Bulk IN endpoint 0x81, Bulk OUT endpoint 0x02, 512-byte max packet

This does not establish an Actions ADFU command interface. Do not send vendor-specific SCSI commands based solely on the manufacturer string "ACTIONS".
