package reorder

import chisel3._
import chisel3.util._

class HazardUnit extends Module {
  val io = IO(new Bundle{
    val alu_p0 =   Input(Bool())
    val renamer =  Input(Bool())

    val stall =   Output(Bool())
  })

  io.stall := io.alu_p0 | io.renamer
}
