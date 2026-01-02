import chisel3._
import chisel3.util._

class Alu extends Module {
  val io = IO(new Bundle{
    val src1 =  Input(UInt(32.W))
    val src2 =  Input(UInt(32.W))
    val op =    Input(UInt(4.W))

    val dest =  Output(UInt(32.W))
    val flags = Output(UInt(3.W)) // 0: equality, 1: less than signed, 2: less than unsigned
  })

  io.dest := 0.U
  switch (io.op) {
    is ("b0000".U) {
      // Add
      io.dest := io.src1 + io.src2
    }
    is ("b0001".U) {
      // Sll
      io.dest := io.src1 << io.src2(4,0)
    }
    is ("b0010".U) {
      // Slt
      io.dest := (io.src1.asSInt < io.src2.asSInt).asUInt
    }
    is ("b0011".U) {
      // Sltu
      io.dest := (io.src1 < io.src2).asUInt
    }
    is ("b0100".U) {
      // Xor
      io.dest := io.src1 ^ io.src2
    }
    is ("b0101".U) {
      // Srl
      io.dest := io.src1 >> io.src2(4,0)
    }
    is ("b0110".U) {
      // Or
      io.dest := io.src1 | io.src2
    }
    is ("b0111".U) {
      // And
      io.dest := io.src1 & io.src2
    }
    is ("b1000".U) {
      // Sub
      io.dest := io.src1 - io.src2
    }
    is ("b1101".U) {
      // Sra
      io.dest := (io.src1.asSInt >> io.src2(4,0)).asUInt
    }
  }

  // Generate flags
  io.flags := Cat(Seq(
    io.src1 < io.src2,
    io.src1.asSInt < io.src2.asSInt,
    io.src1 === io.src2
  ))
}

/*object Alu extends App {
  ChiselStage.emitSystemVerilogFile(
    new Alu,
    firtoolOpts = Array()
  )
}*/
