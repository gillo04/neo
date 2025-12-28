# Uncontitional jump foreward and backwards

.section .text
.globl _start

_start:
  li a0, 1        # 0
  li a1, 10       # 1
  li a2, 100      # 2
  li a3, 1000     # 3
  j label_1       # 4

label_2:
  add a4, a4, a2  # 5
  j label_3       # 6

label_1:
  add a4, a0, a1  # 7
  j label_2       # 8

label_3:
  add a4, a4, a3  # 9
  j .
