# Uncontitional jump foreward and backwards

.section .text
.globl _start

_start:
  li a0, 5            # 0
  mv a1, x0           # 4
  li a2, 3            # 8

loop:
  add a1, a1, a0      # 12
  addi a2, a2, -1     # 16
  bgt a2, x0, loop    # 20

  li a1, 50           # 24
  li a3, 15           # 28
  bne a1, a3, label1  # 32
  
  li a1, 60           # 36
  j exit              # 40

label1:
  li a1, 70           # 44
  j exit              # 48

  li a1, 80           # 52

exit:
  j .                 # 56
