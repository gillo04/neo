# Calculate the 10th fibonacci number, 89
# The result is moved to a4

.section .text
.globl _start

_start:
  mv a0, x0 # Pointer to the array

  # Initalize array [0, 1]
  sw x0, 0(x0)
  li a1, 1
  sw a1, 4(x0)
  
  li a1, 10 # Counter

loop:
  lw a2, 0(a0)
  lw a3, 4(a0)
  add a2, a2, a3
  sw a2, 8(a0)
  addi a0, a0, 4
  addi a1, a1, -1
  bgt a1, x0, loop

  lw a4, 4(a0)
  j .
