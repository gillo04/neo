# Read and write to memory

.section .text
.globl _start

_start:
  li a0, 1

  # [0] <- a1
  li a1, 1
  sb a1, -1(a0)

  # [1] <- a1
  li a1, 2 
  sb a1, 0(a0)

  # [2] <- a1
  li a1, 3 
  sb a1, 1(a0)

  # [3] <- a1
  li a1, 4 
  sb a1, 2(a0)

  j .
