package reorder

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import java.nio.file.{Files, Paths}
import java.nio.{ByteBuffer, ByteOrder}

class RenamerTest extends AnyFreeSpec with Matchers with ChiselSim {
  "Should rename registers properly" in {
    simulate(new Renamer(6, 2, 2)) { c =>
      // Rename x1, x2, x3, x4
      c.io.dest_valid.poke(true.B)
      c.io.dest.poke(0.U)
      c.io.dest_addr.expect(0.U)
      c.clock.step()
      for (i <- 1 until 5) {
        c.io.dest.poke(i.U)
        c.io.dest_addr.expect((i - 1).U)
        c.clock.step()
      }
      c.io.dest_valid.poke(false.B)
      c.clock.step()

      // Validated x2, x3
      c.io.pip_ports(0).valid.poke(true.B)
      c.io.pip_ports(0).addr.poke(1.U)
      c.io.pip_ports(0).dest.poke(2.U)
      c.io.pip_ports(0).value.poke(52.U)
      c.clock.step()

      c.io.pip_ports(0).addr.poke(2.U)
      c.io.pip_ports(0).dest.poke(3.U)
      c.io.pip_ports(0).value.poke(53.U)
      c.clock.step()
      c.io.pip_ports(0).valid.poke(false.B)
      
      // Read x2, x3 and expect the correct value
      c.io.srcs(0).poke(2.U)
      c.io.srcs(1).poke(3.U)
      c.io.vals(0).expect(52.U)
      c.io.vals(1).expect(53.U)
      c.io.stall.expect(false.B)

      // Read x4 and expect a stall
      c.io.srcs(0).poke(4.U)
      c.io.srcs(1).poke(0.U)
      c.io.stall.expect(true.B)
      c.io.srcs(0).poke(0.U)
      c.io.srcs(1).poke(0.U)

      // Validate x1, x4 and wait 3 cycles
      c.io.pip_ports(0).valid.poke(true.B)
      c.io.pip_ports(0).addr.poke(0.U)
      c.io.pip_ports(0).dest.poke(1.U)
      c.io.pip_ports(0).value.poke(51.U)

      c.io.pip_ports(1).valid.poke(true.B)
      c.io.pip_ports(1).addr.poke(3.U)
      c.io.pip_ports(1).dest.poke(4.U)
      c.io.pip_ports(1).value.poke(54.U)
      c.clock.step()
      c.io.pip_ports(0).valid.poke(false.B)
      c.io.pip_ports(1).valid.poke(false.B)

      c.clock.step()
      c.clock.step()
      c.clock.step()

      // Read x1, x2, x3, x4 and expect the correct value
      for (i <- 1 until 5) {
        c.io.srcs(0).poke(i.U)
        c.io.vals(0).expect((50 + i).U)
        c.io.stall.expect(false.B)
        c.clock.step()
      }
      c.io.srcs(0).poke(0.U)
    }
  }

  "Should handle ROB maxout" in {
    simulate(new Renamer(6, 2, 2)) { c =>
      // Rename x1 64 times
      c.io.dest_valid.poke(true.B)
      c.io.dest.poke(1.U)
      for (i <- 0 until 64) {
        c.io.dest_addr.expect(i.U)
        c.io.stall.expect(false.B)
        c.clock.step()
      }

      // Try to rename x1 and expect a stall
      c.io.stall.expect(true.B)
      c.io.dest_valid.poke(false.B)
      c.io.stall.expect(false.B)
      c.clock.step()

      // Validate 1 x1
      c.io.pip_ports(0).valid.poke(true.B)
      c.io.pip_ports(0).addr.poke(0.U)
      c.io.pip_ports(0).dest.poke(1.U)
      c.io.pip_ports(0).value.poke(52.U)
      c.clock.step()
      c.io.pip_ports(0).valid.poke(false.B)
      c.clock.step()

      // Rename x1 and expect no stall
      c.io.dest_valid.poke(true.B)
      c.io.dest.poke(1.U)
      c.io.dest_addr.expect(0.U)
      c.io.stall.expect(false.B)
      c.clock.step()

      // Try to rename x1 and expect a stall
      c.io.stall.expect(true.B)
      c.io.dest_valid.poke(false.B)
      c.io.stall.expect(false.B)
      c.clock.step()

      // Rename x0 65 times without stalls
      c.io.dest_valid.poke(true.B)
      c.io.dest.poke(0.U)
      for (i <- 0 until 65) {
        c.io.dest_addr.expect(1.U)
        c.io.stall.expect(false.B)
        c.clock.step()
      }
    }
  }
}
