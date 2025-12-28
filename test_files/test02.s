# Return address

.section .text
.globl _start

_start:
  nop
  nop
  jal a0, label
  nop
  nop
  nop
label:
  j .
