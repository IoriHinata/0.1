CROSS ?= mipsel-linux-gnu-
CC := $(CROSS)gcc
OBJCOPY := $(CROSS)objcopy
OBJDUMP := $(CROSS)objdump
CFLAGS := -EL -mips32 -mabi=32 -mno-abicalls -fno-pic -G0 -Os \
          -ffreestanding -fno-builtin -fno-stack-protector \
          -fno-asynchronous-unwind-tables -Wall -Wextra
LDFLAGS := -EL -mips32 -mabi=32 -nostdlib -nostartfiles -nodefaultlibs \
           -Wl,-T,linker.ld -Wl,-Map,colibri-z4.map -Wl,--build-id=none
.PHONY: all clean
all: colibri-z4.elf colibri-z4.bin colibri-z4.dis
start.o: boot/start.S
	$(CC) $(CFLAGS) -c $< -o $@
kernel.o: kernel.c
	$(CC) $(CFLAGS) -c $< -o $@
colibri-z4.elf: start.o kernel.o linker.ld
	$(CC) $(CFLAGS) $(LDFLAGS) start.o kernel.o -o $@
colibri-z4.bin: colibri-z4.elf
	$(OBJCOPY) -O binary $< $@
colibri-z4.dis: colibri-z4.elf
	$(OBJDUMP) -d $< > $@
clean:
	rm -f *.o *.elf *.bin *.map *.dis
