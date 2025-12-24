riscv64-linux-gnu-as -march=rv64im -mabi=lp64 $@.s -o $@.o
riscv64-linux-gnu-ld -T link.ld $@.o -o $@.elf
riscv64-linux-gnu-objcopy -O binary $@.elf $@.bin

rm $@.o
rm $@.elf

