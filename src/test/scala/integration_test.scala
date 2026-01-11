import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import java.nio.file.{Files, Paths}
import java.nio.{ByteBuffer, ByteOrder}

class IntegrationTest extends AnyFreeSpec with Matchers with ChiselSim {
  def instructionsFromFile(path_str: String): Array[Int] = {
    val path = Paths.get(path_str)
    val bytes = Files.readAllBytes(path)

    val intBuffer = ByteBuffer
      .wrap(bytes)
      .order(ByteOrder.LITTLE_ENDIAN)
      .asIntBuffer()

    val instructions = new Array[Int](intBuffer.remaining())
    intBuffer.get(instructions)

    return instructions
  }

  "7: Fibonacci with two registers" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test07.bin")

    simulate(new Integration) { c =>
      c.io.rf(14).expect(89.U)
      // Load memory
      c.io.wen.poke(true.B)
      for (i <- 0 until instruction_cache.size) {
        c.io.addr.poke(i.U)
        c.io.wdata.poke(instruction_cache(i).S(32.W).asUInt)
        c.clock.step()
      }

      for (i <- instruction_cache.size until 1024) {
        c.io.addr.poke(i.U)
        c.io.wdata.poke(0.U)
        c.clock.step()
      }
      c.io.wen.poke(false.B)
      println("Loaded")

      // Go to 0
      while (c.io.pc.peek().litValue % (1024 * 4) != 0) {
        println(f"${c.io.pc.peek()}")
        c.clock.step()
      }

      // Execute
      c.clock.step()
      c.clock.step()
      c.clock.step()
      c.clock.step()
      c.clock.step()
      while (c.io.rf(14).peek().litValue == 0) {
        // Fetch instruction
        val pc = c.io.pc.peek().litValue.toInt/4
        if (pc < instruction_cache.size) {
          c.io.inst_in.poke(instruction_cache(pc).S(32.W).asUInt)
        } else {
          c.io.inst_in.poke(0.U)
        }

        // Step the clock
        c.clock.step()
        // println(f"${c.io.stall.peek()}\t${c.io.rd_p0.peek()}\t${c.io.rd_p1.peek()}\t${c.io.rd_p2.peek()}")
        // c.io.rf(10).expect(expected_a0(i))
      }
      println(f"${c.io.rf(14).peek()}")
      c.io.rf(14).expect(89.U)
    }
  }
}
