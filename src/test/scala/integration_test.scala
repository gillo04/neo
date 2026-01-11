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
      
      // Execute
      while (c.io.rf(14).peek().litValue == 0) {
        // Step the clock
        c.clock.step()
      }
      c.io.rf(14).expect(89.U)
    }
  }

  "8: Fibonacci with array" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test08.bin")

    simulate(new Integration) { c =>
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
      
      // Execute
      while (c.io.rf(14).peek().litValue == 0) {
        // Step the clock
        c.clock.step()
      }
      c.io.rf(14).expect(89.U)
    }
  }
}
