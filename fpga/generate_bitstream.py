import os
import sys

# run source /tools/Xilinx/Vivado/2023.1/settings64.sh before executing

# Generate bitstream
os.system("rm -r ./vivado_project_basys/")
os.system("vivado -nojournal -nolog -mode batch -source ./generate_bitstream.tcl")
os.system("cp ./vivado_project_basys/vivado_project_basys.runs/impl_1/Integration.bit ./build/")

# Generate MMI file
bram_location = ""
with open("./build/bram_info.txt", "r") as file:
    for line in file:
        if line.strip() != "":
            bram_location = line.strip()

placement = bram_location.split("_")[1]

mmi = f"""
<?xml version="1.0" encoding="UTF-8"?>
<MemInfo Version="1" Minor="0">
    <Processor Endianness="Little" InstPath="Basys3">
        <AddressSpace Name="i_cache" Begin="0" End="4095" CoreMemory_Width="32">
            <BusBlock>
                <BitLane MemType="RAMB32" Placement="{placement}">
                    <DataWidth MSB="31" LSB="0"/>
                    <AddressRange Begin="0" End="4095"/>
                    <Parity ON="false" NumBits="0"/>
                </BitLane>
            </BusBlock>
        </AddressSpace>
    </Processor>
    <Config>
        <Option Name="Part" Val="xc7a35tcpg236-1"/>
    </Config>
</MemInfo>
"""

with open("./build/memory_map.mmi", "w") as file:
    file.write(mmi)
