package out_of_order 

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

class Scheduler(rob_addr_bits: Int, pip_ports_count: Int, inputs: Int) extends Module {
  val buffer_size = math.pow(2, rob_addr_bits).toInt

  val io = IO(new Bundle{
    val stall =     Output(Bool())
    val valid_inst =Input(Bool())
    val inst =      Input(new Control)
    
    // Request ports
    val vals =      Output(Vec(inputs, UInt(32.W)))

    // Destination
    val dest_addr = Output(UInt(rob_addr_bits.W))

    // Issue
    val issue =     Output(new Control)

    // Update ports
    val pip_ports = Input(Vec(pip_ports_count, new RobPipPort(rob_addr_bits)))

    // Debug io
    val registers = Output(Vec(32, new RfEntry(rob_addr_bits)))
    val buffer =    Output(Vec(buffer_size, new RobEntry))
  })

  val rf = Module(new RegisterFile(rob_addr_bits, inputs))
  io.registers := rf.io.registers

  val rob = Module(new Rob(rob_addr_bits, pip_ports_count, inputs))
  io.buffer := rob.io.buffer

  // TODO: make the 3 parametric
  val rs = Module(new ReservationStation(3, rob_addr_bits, pip_ports_count))

  // Rob writeback
  rob.io.pip_ports := io.pip_ports
  rf.io.write_reg := rob.io.rf_dest
  rf.io.write_data.valid := rob.io.rf_valid
  rf.io.write_data.value := rob.io.rf_value
  rf.io.write_data.name := rob.io.rf_name

  // Search for value (for the issued instruction)
  // Pipeline the RS to the RF
  val pip0 = RegNext(rs.io.issue,
    (new RsEntry(rob_addr_bits)).Lit(
      _.valid -> false.B,
      _.s1_valid -> false.B,
      _.s1_name -> 0.U,
      _.s2_valid -> false.B,
      _.s2_name -> 0.U,
      _.d_name -> 0.U,
      _.inst -> (new Control).Lit(
        _.src1 -> 0.U,
        _.src2 -> 0.U,
        _.dest -> 0.U,
        _.imm -> 0.U,
        _.alu_op -> 0.U,
        _.imm_mux -> false.B,
        _.mem_mux -> false.B,
        _.mem_size -> 0.U,
        _.mem_sx -> false.B,
        _.mem_store -> false.B,
        _.alu_d -> false.B,
        _.dest_valid_0 -> false.B,
        _.dest_valid_1 -> false.B,
      )
    )
  )
  rf.io.srcs(0) := pip0.inst.src2
  // rob.io.srcs(0) := rf.io.dests(0).name
  // io.vals(0) := Mux(rf.io.dests(0).valid, rf.io.dests(0).value, rob.io.dests(0).value)
  rob.io.srcs(0) := pip0.s1_name
  io.vals(0) := rob.io.dests(0).value

  rf.io.srcs(1) := pip0.inst.src1
  // rob.io.srcs(1) := rf.io.dests(1).name
  // io.vals(1) := Mux(rf.io.dests(1).valid, rf.io.dests(1).value, rob.io.dests(1).value)
  rob.io.srcs(1) := pip0.s2_name
  io.vals(1) := rob.io.dests(1).value

  io.dest_addr := RegNext(rs.io.dest_name, 0.U)
  io.issue := pip0.inst

  // Stall
  val stall = Seq.tabulate(inputs)(i => i).foldLeft(false.B)((x, y) => x | (!rob.io.dests(y).valid & !rf.io.dests(y).valid)) |
              (!rob.io.rq_valid /*& rs.io.rob_ready avoid combinational loop*/) | !rs.io.rs_ready
  io.stall := stall
  rf.io.stall := stall

  // Request entry
  rob.io.rq_ready := !stall & rs.io.rob_ready
  rs.io.rob_addr := rob.io.rq_addr
  rs.io.rob_valid := rob.io.rq_valid

  // Update the requested register
  rf.io.dest := rs.io.rf_dest
  rf.io.dest_valid := rs.io.rf_dest_valid
  rf.io.new_name := rs.io.rf_new_name

  // Connect the reservation station
  rs.io.inst := io.inst
  rs.io.valid_inst := io.valid_inst

  rf.io.rs_src1 := rs.io.rf_src1
  rf.io.rs_src2 := rs.io.rf_src2

  rs.io.rf_name1 := rf.io.rs_entry1.name
  rs.io.rf_valid1 := rf.io.rs_entry1.valid
  rs.io.rf_name2 := rf.io.rs_entry2.name
  rs.io.rf_valid2 := rf.io.rs_entry2.valid

  rs.io.pip_ports := io.pip_ports
}
