package reorder

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import java.nio.file.{Files, Paths}
import java.nio.{ByteBuffer, ByteOrder}

class RobTest extends AnyFreeSpec with Matchers with ChiselSim {
  "Should properly increment pointers" in {
    simulate(new Rob(6, 2)) { c =>
      // Initialize everything
      c.io.pip_ports(0).valid.poke(false.B)
      c.io.pip_ports(0).addr.poke(0.U)
      c.io.pip_ports(0).dest.poke(0.U)
      c.io.pip_ports(0).value.poke(0.U)

      c.io.pip_ports(1).valid.poke(false.B)
      c.io.pip_ports(1).addr.poke(0.U)
      c.io.pip_ports(1).dest.poke(0.U)
      c.io.pip_ports(1).value.poke(0.U)

      var requested = Seq[BigInt]()
      // Request 5 entries
      val ex = 3
      c.io.rq_ready.poke(true.B)
      for (i <- 0 until 5) {
        // print(f"${c.io.buff(ex).valid.peek().litValue} ")
        requested = requested :+ c.io.rq_addr.peek().litValue
        c.io.rq_valid.expect(true.B)
        c.io.rq_addr.expect(i.U)
        c.clock.step()
      }

      // Wait 3 cycles without requesting
      c.io.rq_ready.poke(false.B)
      for (i <- 0 until 3) {
        c.io.rq_valid.expect(true.B)
        c.io.rq_addr.expect(5.U)
        c.clock.step()
      }

      // Make 3 entries valid
      c.io.pip_ports(0).valid.poke(true.B)
      for (i <- 0 until 3) {
        c.io.pip_ports(0).addr.poke((2 - i).U)
        c.io.pip_ports(0).dest.poke(1.U)
        c.io.pip_ports(0).value.poke((42 + 2 - i).U)
        c.io.rf_valid.expect(false.B)
        c.clock.step()
      }
      c.io.pip_ports(0).valid.poke(false.B)

      // Consume the 3 valid entries and stop
      for (i <- 0 until 3) {
        c.io.rf_valid.expect(true.B)
        c.io.rf_dest.expect(1.U)
        c.io.rf_value.expect((42 + i).U)
        c.clock.step()
      }
      c.io.rf_valid.expect(false.B)
      c.clock.step()

      // Max out the entries
      c.io.rq_ready.poke(true.B)
      for (i <- 0 until 64 - 2) {
        requested = requested :+ c.io.rq_addr.peek().litValue
        c.io.rq_valid.expect(true.B)
        c.io.rq_addr.expect(((i + 5) % 64).U)
        c.clock.step()
      }
      c.io.rq_ready.poke(false.B)
      c.io.rq_valid.expect(false.B)
      c.clock.step()

      // Validate all entries and consume them
      c.io.pip_ports(0).valid.poke(true.B)
      for (i <- 0 until 64) {
        c.io.pip_ports(0).addr.poke(((3 + i) % 64).U)
        c.io.pip_ports(0).dest.poke(1.U)
        c.io.pip_ports(0).value.poke((42 + i).U)

        c.clock.step()

        c.io.rf_valid.expect(true.B)
        c.io.rf_dest.expect(1.U)
        c.io.rf_value.expect((42 + i).U)
      }
      c.clock.step()
      c.io.pip_ports(0).valid.poke(false.B)

      // Request 5 more
      c.io.rq_ready.poke(true.B)
      for (i <- 0 until 5) {
        c.io.rq_valid.expect(true.B)
        c.io.rq_addr.expect(((i + 3) % 64).U)
        c.clock.step()
      }
      c.io.rq_ready.poke(false.B)
      c.clock.step()
    }
  }
}
