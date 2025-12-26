import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import java.nio.file.{Files, Paths}
import java.nio.{ByteBuffer, ByteOrder}

class InOrderTest extends AnyFreeSpec with Matchers with ChiselSim {
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

  "0: Addition with dependencies" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test00.bin")
    val expected_a0 = Seq(0.U,  0.U,  0.U,  0.U,  42.U,   42.U,   42.U,   42.U,   42.U,  42.U)
    val expected_a1 = Seq(0.U,  0.U,  0.U,  0.U,  0.U,    100.U,  100.U,  100.U,  142.U, 142.U)

    simulate(new InOrder) { c =>

      // Wait 4 cycles
      for (i <- 0 until expected_a0.size) {
        // Fetch instruction
        val pc = c.io.pc.peek().litValue.toInt
        if (pc < instruction_cache.size) {
          c.io.inst_in.poke(instruction_cache(pc).U)
        } else {
          c.io.inst_in.poke(0.U)
        }

        // Step the clock
        c.clock.step()
        // println(f"${c.io.stall.peek()}\t${c.io.rd_p0.peek()}\t${c.io.rd_p1.peek()}\t${c.io.rd_p2.peek()}")
        c.io.rf(10).expect(expected_a0(i))
        c.io.rf(11).expect(expected_a1(i))
      }

    }
  }
}
