import chisel3._
import chisel3.util._

class RegisterFile(inputs: Int) extends Module {
  val io = IO(new Bundle{
    val srcs =  Input(Vec(inputs, UInt(5.W)))
    val dests = Output(Vec(inputs, UInt(32.W)))
    // val inval =         Input(UInt(5.W))    // Register to invalidate (usually the destination)
    val write_reg =     Input(UInt(5.W))
    val write_data =    Input(UInt(32.W))
    // val stall =         Output(Bool())

    // Debug io
    val registers = Output(Vec(32, UInt(32.W)))
  })

  val registers = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
  // val valid = RegInit(VecInit(Seq.fill(32)(true.B)))
  io.registers := registers

  // Read logic
  for (i <- 0 until inputs) {
    io.dests(i) := registers(io.srcs(i))
  }

  // Write logic
  when (io.write_reg =/= 0.U) {
    registers(io.write_reg) := io.write_data
  }
  
  // Valid and stall logic
  for (i <- 0 until 32) {
    /*when(valid(i)) {
      valid(i) := i.U =/= io.inval
    } .otherwise {
      valid(i) := i.U === io.write_reg
    }*/
  }
  // io.stall := !valid.reduce((v1, v2) => v1 & v2)
}
