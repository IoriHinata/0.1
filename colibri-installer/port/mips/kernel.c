#include <stdint.h>
#define COLIBRI_MAGIC 0x434F4C49u
#define COLIBRI_ABI 1u

struct boot_record {
    volatile uint32_t magic, abi, state, ticks, last_error;
    volatile char message[64];
};
__attribute__((section(".noinit"), used))
volatile struct boot_record colibri_boot;

static void set_message(const char *s) {
    unsigned i = 0;
    for (; i < sizeof(colibri_boot.message) - 1 && s[i]; ++i)
        colibri_boot.message[i] = s[i];
    colibri_boot.message[i] = 0;
}

void colibri_main(void) {
    colibri_boot.magic = COLIBRI_MAGIC;
    colibri_boot.abi = COLIBRI_ABI;
    colibri_boot.state = 1;
    colibri_boot.ticks = 0;
    colibri_boot.last_error = 0;
    set_message("ColibriOS MIPS kernel reached C entry");
    for (;;) {
        __asm__ volatile ("wait");
        ++colibri_boot.ticks;
    }
}
