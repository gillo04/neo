# LUI AUIPC
# a0 = 4096
# a1 = 4100

.section .text
.globl _start

_start:
  lui a0, 1
  auipc a1, 1
  j .
