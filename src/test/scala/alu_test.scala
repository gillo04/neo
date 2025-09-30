import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class AluTest extends AnyFreeSpec with Matchers with ChiselSim {
  "Alu should perform all operations" in {
    simulate(new Alu) { c =>
      // Add
      c.io.src1.poke(5.U)
      c.io.src2.poke(10.U)
      c.io.op.poke("b0000".U)
      c.io.dest.expect(15.U)
      c.clock.step()

      // Sub
      c.io.src1.poke(10.U)
      c.io.src2.poke(5.U)
      c.io.op.poke("b1000".U)
      c.io.dest.expect(5.U)
      c.clock.step()

      // And
      c.io.src1.poke(1.U)
      c.io.src2.poke(3.U)
      c.io.op.poke("b0111".U)
      c.io.dest.expect(1.U)
      c.clock.step()

      // Or
      c.io.src1.poke(1.U)
      c.io.src2.poke(2.U)
      c.io.op.poke("b0110".U)
      c.io.dest.expect(3.U)
      c.clock.step()

      // Xor
      c.io.src1.poke(1.U)
      c.io.src2.poke(3.U)
      c.io.op.poke("b0100".U)
      c.io.dest.expect(2.U)
      c.clock.step()

      // Sll
      c.io.src1.poke(1.U)
      c.io.src2.poke(3.U)
      c.io.op.poke("b0001".U)
      c.io.dest.expect(8.U)
      c.clock.step()

      // Srl
      c.io.src1.poke(8.U)
      c.io.src2.poke(3.U)
      c.io.op.poke("b0101".U)
      c.io.dest.expect(1.U)
      c.clock.step()

      // Sra
      c.io.src1.poke("b1111111111111111111111111111111111111111111111111111111111111110".U)
      c.io.src2.poke(1.U)
      c.io.op.poke("b1101".U)
      c.io.dest.expect("b1111111111111111111111111111111111111111111111111111111111111111".U)
      c.clock.step()

      // Slt
      c.io.src1.poke(5.U)
      c.io.src2.poke((-10).S(64.W).asUInt)
      c.io.op.poke("b0010".U)
      c.io.dest.expect(0.U)
      c.clock.step()

      // Sltu
      c.io.src1.poke(5.U)
      c.io.src2.poke((-10).S(64.W).asUInt)
      c.io.op.poke("b0011".U)
      c.io.dest.expect(1.U)
      c.clock.step()

    }
  }
}
