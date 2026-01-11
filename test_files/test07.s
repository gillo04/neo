# Calculate the 10th fibonacci number, 89
# The result is moved to a4

.section .text
.globl _start

_start:
  # initialize a0, a1, a2
  mv a4, x0
  mv a0, x0
  li a1, 1
  li a2, 10 # Counter

loop:
  add a3, a0, a1
  mv a0, a1
  mv a1, a3
  addi a2, a2, -1
  bgt a2, x0, loop

  mv a4, a1
  j .
