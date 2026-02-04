# Create project
# 
create_project vivado_project_basys ./vivado_project_basys -part xc7a35tcpg236-1
set_property board_part digilentinc.com:basys3:part0:1.2 [current_project]
add_files -norecurse {../builds/out_of_order/Rob.sv
                      ../builds/out_of_order/Alu.sv
                      ../builds/out_of_order/HazardUnit.sv
                      ../builds/out_of_order/Integration.sv
                      ../builds/out_of_order/Cache.sv
                      ../builds/out_of_order/OutOfOrder.sv
                      ../builds/out_of_order/Fetch.sv
                      ../builds/out_of_order/mem_1024x32.sv
                      ../builds/out_of_order/Mau.sv
                      ../builds/out_of_order/Scheduler.sv
                      ../builds/out_of_order/RegisterFile.sv
                      ../builds/out_of_order/ReservationStation.sv}
import_files -force -norecurse
import_files -fileset constrs_1 -force -norecurse ./constraints.xdc
update_compile_order -fileset sources_1
update_compile_order -fileset sources_1

# Generate bitstreal
launch_runs synth_1 -jobs 6
wait_on_run synth_1
launch_runs impl_1 -jobs 6
wait_on_run impl_1
launch_runs impl_1 -to_step write_bitstream -jobs 6
wait_on_run impl_1

# Extract BRAM locations
open_run impl_1

# Cleare the contents of bram_info.txt
set fp_w [open "./build/bram_info.txt" "w"]
puts $fp_w ""

set fp_a [open "./build/bram_info.txt" "a"]
puts $fp_a [get_property LOC [get_cells i_cache/mem_ext/Memory_reg]]
