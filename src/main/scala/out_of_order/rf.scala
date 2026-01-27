package out_of_order 

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

class RfEntry(rob_addr_bits: Int) extends Bundle {
  val valid =     Bool()
  val value =     UInt(32.W)
  val name =      UInt(rob_addr_bits.W)
}

class RfSrc(rob_addr_bits: Int) extends Bundle {
  val reg =       UInt(5.W)
  val name =      UInt(rob_addr_bits.W)
}

class RegisterFile(rob_addr_bits: Int, inputs: Int) extends Module {
  val io = IO(new Bundle{
    // Read sources
    val srcs =          Input(Vec(inputs, UInt(5.W)))
    val dests =         Output(Vec(inputs, new RfEntry(rob_addr_bits)))

    // Retire result
    val write_reg =     Input(UInt(5.W))
    val write_data =    Input(new RfEntry(rob_addr_bits))
    val stall =         Input(Bool())

    // Destination 
    val dest =          Input(UInt(5.W))
    val dest_valid =    Input(Bool())
    val new_name =      Input(UInt(rob_addr_bits.W))

    // Reservation station
    val rs_src1 =       Input(UInt(5.W))
    val rs_src2 =       Input(UInt(5.W))
    val rs_entry1 =     Output(new RfEntry(rob_addr_bits))
    val rs_entry2 =     Output(new RfEntry(rob_addr_bits))

    // Debug io
    val registers = Output(Vec(32, new RfEntry(rob_addr_bits)))
  })

  val registers = RegInit(VecInit(Seq.fill(32)((new RfEntry(rob_addr_bits)).Lit(_.valid -> true.B, _.value -> 0.U, _.name -> 0.U))))
  io.registers := registers

  // Read logic
  for (i <- 0 until inputs) {
    io.dests(i) := registers(io.srcs(i))
    /*when (io.write_reg =/= 0.U && !io.stall) {
      registers(io.srcs(i).reg).valid := false.B
      registers(io.srcs(i).reg).name := io.srcs(i).name
    }*/
  }

  // Write logic
  when (io.write_data.valid &&
        registers(io.write_reg).name === io.write_data.name &&
        (io.write_reg =/= io.dest || !io.dest_valid) &&
        io.write_reg =/= 0.U) {
    registers(io.write_reg).value := io.write_data.value
    registers(io.write_reg).valid := true.B
  }

  // Dest logic
  when (io.dest =/= 0.U && !io.stall && io.dest_valid) {
    registers(io.dest).valid := false.B
    registers(io.dest).name := io.new_name
  }

  // Reservation station ports
  io.rs_entry1 := registers(io.rs_src1)
  io.rs_entry2 := registers(io.rs_src2)
}
