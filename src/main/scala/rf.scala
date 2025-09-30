import chisel3._
import chisel3.util._

class RegisterFile(inputs: Int) extends Module {
  val io = IO(new Bundle{
    val srcs =  Input(Vec(inputs, UInt(5.W)))
    val dests = Output(Vec(inputs, UInt(5.W)))
    // val src1 =  Input(UInt(5.W))
    // val src2 =  Input(UInt(5.W))
    // val dest1 = Output(UInt(64.W))
    // val dest2 = Output(UInt(64.W))

    val write_reg =     Input(UInt(5.W))
    val write_data =    Input(UInt(64.W))
    val we =            Input(Bool()) // Write enable
  })

  val registers = RegInit(VecInit(Seq.fill(32)(0.U(64.W))))

  // Read logic
  for (i <- 0 until inputs) {
    io.dests(i) := registers(io.srcs(i))
  }

  // Write logic
  when (io.we && io.write_reg =/= 0.U) {
    registers(io.write_reg) := io.write_data
  }
}
