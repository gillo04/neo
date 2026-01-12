package in_order

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class RegisterFileTest extends AnyFreeSpec with Matchers with ChiselSim {
  "Register file should read and write correctly" in {
    simulate(new RegisterFile(2)) { c =>
      for (i <- 0 until 32) {
        c.io.write_reg.poke(i.U)
        c.io.write_data.poke((i + 1).U)
        c.clock.step()
      }
      c.io.write_reg.poke(0.U)

      for (i <- 0 until 32) {
        c.io.srcs(0).poke(i.U)
        c.io.srcs(1).poke(i.U)
        // println(f"${i} ${c.io.dests(0).peek()}")

        if (i == 0) {
          c.io.dests(0).expect(0.U)
          c.io.dests(1).expect(0.U)
        } else {
          c.io.dests(0).expect((i+1).U)
          c.io.dests(1).expect((i+1).U)
        }
        c.clock.step()
      }
    }
  }
}
