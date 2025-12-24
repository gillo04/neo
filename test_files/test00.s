# Simple instruction test
# a0 = 42 + 100

.section .text
.globl _start

_start:
  li a0, 42
  li a1, 100
  add a1, a0, a1
  j .
