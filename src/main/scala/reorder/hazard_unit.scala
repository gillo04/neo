package reorder

import chisel3._
import chisel3.util._

class HazardUnit extends Module {
  val io = IO(new Bundle{
    val alu_p0 =   Input(Bool())
    val renamer =  Input(Bool())

    val stall =   Output(Bool())
    val stall_fetch =   Output(Bool())
  })

  io.stall := io.renamer
  io.stall_fetch := io.alu_p0 | io.renamer
}
