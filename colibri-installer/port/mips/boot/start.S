/* ATJ2127 MIPS32 entry stub; RAM test image, never a flash image. */
    .set noreorder
    .set noat
    .section .text.boot, "ax", @progbits
    .globl _start
    .ent _start
_start:
    la      $sp, __stack_top
    move    $fp, $zero
    la      $t0, __bss_start
    la      $t1, __bss_end
1:
    sltu    $t2, $t0, $t1
    beqz    $t2, 2f
    nop
    sw      $zero, 0($t0)
    addiu   $t0, $t0, 4
    b       1b
    nop
2:
    jal     colibri_main
    nop
3:
    wait
    b       3b
    nop
    .end _start
