# Alu test with dependency
# a1 = 42 + 100

.section .text
.globl _start

_start:
  li a0, 42
  li a1, 100
  add a1, a0, a1
  mv x31, a1
  j .
