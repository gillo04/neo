import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class FetchTest extends AnyFreeSpec with Matchers with ChiselSim {
  "Program counter should increment" in {
    simulate(new Fetch) { c =>
      c.io.inst_in.poke(0.U)
      for (i <- 0 until 10) {
        c.io.pc.expect((i * 4).U)
        c.clock.step()
      }
    }
  }

  "Instructions should be decoded correctly" in {
    simulate(new Fetch) { c =>
      c.io.inst_in.poke("b0000000_00011_00010_000_00001_0110011".U) // ADD r1, r2, r3

      c.io.src1.expect(0.U)
      c.io.src2.expect(0.U)
      c.io.dest.expect(0.U)
      c.io.imm.expect(0.U)
      c.io.alu_op.expect(0.U)
      c.clock.step()
      c.io.src1.expect(2.U)
      c.io.src2.expect(3.U)
      c.io.dest.expect(1.U)
      c.io.imm.expect(0.U)
      c.io.alu_op.expect(0.U)

      c.io.inst_in.poke("b0100000_00011_00010_000_00001_0110011".U) // SUB r1, r2, r3
      c.clock.step()
      c.io.src1.expect(2.U)
      c.io.src2.expect(3.U)
      c.io.dest.expect(1.U)
      c.io.imm.expect(0.U)
      c.io.alu_op.expect("b1000".U)

      c.io.inst_in.poke("b101010101010_00010_000_00001_0010011".U) // ADDI r1, r2, b101010101010
      c.clock.step()
      c.io.src1.expect(2.U)
      c.io.src2.expect(0.U)
      c.io.dest.expect(1.U)
      c.io.imm.expect("b11111111111111111111101010101010".U)
      c.io.alu_op.expect(0.U)

      c.io.inst_in.poke("b0100000_00001_00010_101_00001_0010011".U) // SRAI r1, r2, 1
      c.clock.step()
      c.io.src1.expect(2.U)
      c.io.src2.expect(0.U)
      c.io.dest.expect(1.U)
      c.io.imm.expect(1.U)
      c.io.alu_op.expect("b1101".U)
    }
  }
}
