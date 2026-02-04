# Using the UpdateMEM flow
import os 

# Build MEM file
mem = "@000000\n"
with open("../test_files/test07.bin", "rb") as f:
    offset = 0
    while True:
        chunk = f.read(4)  # 32 bits = 4 bytes
        if not chunk:
            break

        # Convert bytes to hex, zero-padded to 8 hex digits
        instruction = chunk.hex().upper().zfill(8)
        mem += f"{instruction} "
        offset += 4
mem += "\n"

with open("./build/instructions.mem", "w") as file:
    file.write(mem)

# Refill BRAMs
os.system("vivado -nojournal -nolog -mode batch -source update_mem.tcl")
