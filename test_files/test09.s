# Test that leverages the advantages of register renaming
# a4 = 21

.section .text
.globl _start

_start:
  li a1, 1
  li a2, 2
  li a3, 10

loop:
  lw a0, 0(x0)
  add a1, a1, a2
  addi a3, a3, -1
  bgt a3, x0, loop

  mv a4, a1
  j .
