package out_of_order 

import chisel3._
import chisel3.util._

// Memory access unit
class Mau extends Module {
  val io = IO(new Bundle{
    // Pipeline
    val addr_p1 =       Input(UInt(32.W)) // Address in ALU
    val addr_p2 =       Input(UInt(32.W)) // Address in Mem
    val read_p2 =       Output(UInt(32.W))
    val write_p1 =      Input(UInt(32.W))

    // Memory
    val addr_m =        Output(UInt(30.W))
    val read_m =        Input(UInt(32.W))
    val write_m =       Output(UInt(32.W))
    val write_mask_m =  Output(UInt(4.W))

    // Signals
    val mem_size_p1 =   Input(UInt(2.W))
    val mem_sx_p1 =     Input(Bool())      // Sign extend the value read from memory
    val mem_store_p1 =  Input(Bool())

    val mem_size_p2 =   Input(UInt(2.W))
    val mem_sx_p2 =     Input(Bool())      // Sign extend the value read from memory
    val mem_store_p2 =  Input(Bool())
  })

  // Truncate the address to the word
  io.addr_m := io.addr_p1(31, 2)

  // Read logic
  // 32 bits
  val word = io.read_m

  // 16 bits
  val half = Mux(io.addr_p2(1), io.read_m(31,16), io.read_m(15,0))

  // 8 bits
  val byte = MuxLookup(io.addr_p2(1,0), 0.U)(Seq(
    0.U -> io.read_m(7,0),
    1.U -> io.read_m(15,8),
    2.U -> io.read_m(23,16),
    3.U -> io.read_m(31,24)
  ))

  // Sign extension
  val byte_x = Wire(UInt(16.W))
  when (io.mem_sx_p2) {
    val byte_sx = Wire(SInt(16.W))
    byte_sx := byte.asSInt
    byte_x := byte_sx.asUInt
  } .otherwise {
    byte_x := byte
  }

  val half_x = Wire(UInt(32.W))
  val selected_half = Mux(io.mem_size_p2(0), half, byte_x)
  when (io.mem_sx_p2) {
    val half_sx = Wire(SInt(32.W))
    half_sx := selected_half.asSInt
    half_x := half_sx.asUInt
  } .otherwise {
    half_x := selected_half
  }

  io.read_p2 := Mux(io.mem_size_p2(1), word, half_x)

  // Write logic
  io.write_m := 0.U
  io.write_mask_m := 0.U
  when (io.mem_store_p1) {
    // Mask generation
    val byte_mask = UIntToOH(io.addr_p1(1,0))
    val half_oh = UIntToOH(io.addr_p1(1))
    val half_mask = Cat(Seq(half_oh(1), half_oh(1), half_oh(0), half_oh(0)))
    io.write_mask_m := MuxLookup(io.mem_size_p1, 0.U)(Seq(
      0.U -> byte_mask,
      1.U -> half_mask,
      2.U -> "b1111".U,
    ))

    // Input shifting
    io.write_m := MuxLookup(io.mem_size_p1, 0.U)(Seq(
      0.U -> MuxLookup(io.addr_p1(1,0), 0.U)(Seq(
        0.U -> io.write_p1,
        1.U -> (io.write_p1 << 8),
        2.U -> (io.write_p1 << 16),
        3.U -> (io.write_p1 << 24),
      )),
      1.U -> MuxLookup(io.addr_p1(1), 0.U)(Seq(
        0.U -> io.write_p1,
        1.U -> (io.write_p1 << 16),
      )),
      2.U -> io.write_p1
    ))
  }
}
