import chisel3._
import chisel3.util._

class HazardUnit extends Module {
  val io = IO(new Bundle{
    val src1 =    Input(UInt(5.W))
    val src2 =    Input(UInt(5.W))
    val rd_p0 =   Input(UInt(5.W))
    val rd_p1 =   Input(UInt(5.W))

    val stall =   Output(Bool())
  })

  val src1 = UIntToOH(io.src1)
  val src2 = UIntToOH(io.src2)
  val rd_p0 = UIntToOH(io.rd_p0)
  val rd_p1 = UIntToOH(io.rd_p1)

  val overlap_mask = Wire(UInt(32.W))
  overlap_mask := (src1 | src2) & (rd_p0 | rd_p1)

  // When calculationg the stall, ignore register x0 => it's always available
  io.stall := Seq.tabulate(31)(i => i).foldLeft(false.B)((x, y) => x | overlap_mask(y + 1))
}
