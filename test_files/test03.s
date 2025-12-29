# JALR

.section .text
.globl _start

_start:
  li a0, 15         # 0
  jalr a1, 5(a0)    # 4
  add a0, a0, a0    # 8
  nop               # 12
  nop               # 16
label:
  addi a0, a0, 5    # 20
  j .
