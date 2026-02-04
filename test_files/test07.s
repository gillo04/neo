# Calculate the 10th fibonacci number, 89
# The result is moved to a4

.section .text
.globl _start

_start:
  # initialize a0, a1, a2
  mv a4, x0               # 0
  mv a0, x0               # 4
  li a1, 1                # 8
  li a2, 10 # Counter     # 12

loop:
  add a3, a0, a1          # 16
  mv a0, a1               # 20
  mv a1, a3               # 24
  addi a2, a2, -1         # 28
  bgt a2, x0, loop        # 32

  mv a4, a1
  mv x31, a1
  j .
