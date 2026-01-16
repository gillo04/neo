package reorder

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

class Renamer(rob_addr_bits: Int, pip_ports_count: Int, inputs: Int) extends Module {
  val io = IO(new Bundle{
    val stall =     Output(Bool())
    
    // Request ports
    val srcs =      Input(Vec(inputs, UInt(5.W)))
    val vals =      Output(Vec(inputs, UInt(32.W)))

    // Destination
    val dest =      Input(UInt(5.W))
    val dest_valid =Input(Bool())
    val dest_addr = Output(UInt(rob_addr_bits.W))

    // Update ports
    val pip_ports = Input(Vec(pip_ports_count, new RobPipPort(rob_addr_bits)))

    // Debug io
    val registers = Output(Vec(32, UInt(32.W)))
  })

  val rf = Module(new RegisterFile(rob_addr_bits, inputs))
  io.registers := rf.io.registers

  val rob = Module(new Rob(rob_addr_bits, pip_ports_count, inputs))

  // Rob writeback
  rob.io.pip_ports := io.pip_ports
  rf.io.write_reg := rob.io.rf_dest
  rf.io.write_data.valid := rob.io.rf_valid
  rf.io.write_data.value := rob.io.rf_value
  rf.io.write_data.name := 0.U

  // Search for value
  for (i <- 0 until inputs) {
    rf.io.srcs(i) := io.srcs(i)
    rob.io.srcs(i) := rf.io.dests(i).name
    io.vals(i) := Mux(rf.io.dests(i).valid, rf.io.dests(i).value, rob.io.dests(i).value)
  }

  // Stall
  val make_request = io.dest_valid & io.dest =/= 0.U
  val stall = Seq.tabulate(inputs)(i => i).foldLeft(false.B)((x, y) => x | (!rob.io.dests(y).valid & !rf.io.dests(y).valid)) |
              (!rob.io.rq_valid & make_request)
  io.stall := stall
  rf.io.stall := stall

  // Request entry
  rob.io.rq_ready := !stall & make_request

  // Update the requested register
  rf.io.dest := io.dest
  rf.io.dest_valid := io.dest_valid
  rf.io.new_name := rob.io.rq_addr
  io.dest_addr := rob.io.rq_addr
}
