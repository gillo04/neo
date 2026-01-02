import chisel3._
import chisel3.util._

// Memory access unit
class Mau extends Module {
  val io = IO(new Bundle{
    // Pipeline
    val addr_p =      Input(UInt(32.W))
    val read_p =      Output(UInt(32.W))
    val write_p =     Input(UInt(32.W))

    // Memory
    val addr_m =      Output(UInt(32.W))
    val read_m =      Input(UInt(32.W))
    val write_m =     Output(UInt(32.W))

    // Signals
    val mem_size =    Input(UInt(2.W))
    val mem_sx =      Input(Bool())      // Sign extend the value read from memory
  })

  io.addr_m := io.addr_p
  io.write_m := io.write_p

  io.read_p := io.read_m
  /*switch (io.mem_size(1,0)) {
    is ("b00".U) {
      io.read_p := io.read_m
    }
  }*/

}
