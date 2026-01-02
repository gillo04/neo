# Uncontitional jump foreward and backwards

.section .text
.globl _start

_start:
  li a0, 5
  mv a1, x0
  li a2, 3

loop:
  add a1, a1, a0
  addi a2, a2, -1
  bgt a2, x0, loop

  li a1, 50
  li a3, 15
  bne a1, a3, label1
  
  li a1, 60
  j exit

label1:
  li a1, 70
  j exit

  li a1, 80

exit:
  j .
