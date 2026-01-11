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
    val expected_a0 = Seq(0.U,  0.U,  0.U,  0.U,  0.U,  42.U,   42.U,   42.U,   42.U,   42.U,  42.U)
    val expected_a1 = Seq(0.U,  0.U,  0.U,  0.U,  0.U,  0.U,    100.U,  100.U,  100.U,  142.U, 142.U)

    simulate(new InOrder) { c =>
      for (i <- 0 until expected_a0.size) {
        // Fetch instruction
        val pc = c.io.pc.peek().litValue.toInt/4
        // Step the clock
        c.clock.step()
        if (pc < instruction_cache.size) {
          c.io.inst_in.poke(instruction_cache(pc).S(32.W).asUInt)
        } else {
          c.io.inst_in.poke(0.U)
        }

        // println(f"${c.io.stall.peek()}\t${c.io.rd_p0.peek()}\t${c.io.rd_p1.peek()}\t${c.io.rd_p2.peek()}")
        c.io.rf(10).expect(expected_a0(i))
        c.io.rf(11).expect(expected_a1(i))
      }

    }
  }

  "1: Unconditional branching" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test01.bin")
    val expected_a4 = Seq(0.U,    0.U,    0.U,    0.U,    0.U,    0.U,    0.U,    0.U,  0.U,  0.U,  0.U,  
                          11.U,   11.U,   11.U,   111.U,  111.U,  111.U, 
                          1111.U, 1111.U, 1111.U, 1111.U, 1111.U, 1111.U)

    simulate(new InOrder) { c =>
      for (i <- 0 until expected_a4.size) {
        // Fetch instruction
        val pc = c.io.pc.peek().litValue.toInt/4
        // Step the clock
        c.clock.step()
        if (pc < instruction_cache.size) {
          c.io.inst_in.poke(instruction_cache(pc).S(32.W).asUInt)
        } else {
          c.io.inst_in.poke(0.U)
        }

        // println(f"pc:${c.io.pc.peek().litValue/4}\td:${c.io.debug.peek().litValue}\ts:${c.io.stall.peek()}\ta0: ${c.io.rf(10).peek().litValue}\ta1: ${c.io.rf(11).peek().litValue}\ta2: ${c.io.rf(12).peek().litValue}\ta3: ${c.io.rf(13).peek().litValue}\ta4: ${c.io.rf(14).peek().litValue}")
        c.io.rf(14).expect(expected_a4(i))
      }
    }
  }

  "2: JAL saves return address" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test02.bin")
    val expected_a0 = Seq(0.U,  0.U,    0.U,    0.U,    0.U,    0.U,    0.U,  12.U,  12.U,  12.U)

    simulate(new InOrder) { c =>
      for (i <- 0 until expected_a0.size) {
        // Fetch instruction
        val pc = c.io.pc.peek().litValue.toInt/4
        // Step the clock
        c.clock.step()
        if (pc < instruction_cache.size) {
          c.io.inst_in.poke(instruction_cache(pc).S(32.W).asUInt)
        } else {
          c.io.inst_in.poke(0.U)
        }

        // println(f"${c.io.rf(10).peek()}\t${c.io.pc.peek()}")
        c.io.rf(10).expect(expected_a0(i))
      }
    }
  }

  "3: JALR" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test03.bin")
    val expected_a0 = Seq(0.U,    0.U,    0.U,    0.U,    0.U,    15.U,   15.U,   15.U,   15.U,   15.U,   15.U,   15.U,   20.U,   20.U)
    val expected_a1 = Seq(0.U,    0.U,    0.U,    0.U,    0.U,    0.U,    0.U,    0.U,    0.U,    0.U,    0.U,    0.U,    0.U,    0.U)

    simulate(new InOrder) { c =>
      for (i <- 0 until expected_a0.size) {
        // Fetch instruction
        val pc = c.io.pc.peek().litValue.toInt/4
        // Step the clock
        c.clock.step()
        if (pc < instruction_cache.size) {
          c.io.inst_in.poke(instruction_cache(pc).S(32.W).asUInt)
        } else {
          c.io.inst_in.poke(0.U)
        }

        // println(f"${c.io.rf(10).peek()}\t${c.io.pc.peek()}")
        c.io.rf(10).expect(expected_a0(i))
      }
    }
  }

  "4: LUI and AUIPC" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test04.bin")
    val expected_a0 = Seq(0.U,    0.U,    0.U,    0.U,    0.U,    4096.U,   4096.U,   4096.U)
    val expected_a1 = Seq(0.U,    0.U,    0.U,    0.U,    0.U,    0.U,      4100.U,   4100.U) 

    simulate(new InOrder) { c =>
      for (i <- 0 until expected_a0.size) {
        // Fetch instruction
        val pc = c.io.pc.peek().litValue.toInt/4
        // Step the clock
        c.clock.step()
        if (pc < instruction_cache.size) {
          c.io.inst_in.poke(instruction_cache(pc).S(32.W).asUInt)
        } else {
          c.io.inst_in.poke(0.U)
        }

        c.io.rf(10).expect(expected_a0(i))
        c.io.rf(11).expect(expected_a1(i))
      }
    }
  }

  "5: Conditional branching" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test05.bin")
    val expected_a1 = Seq(0.U,    0.U,    0.U,    0.U,    0.U,    0.U,  0.U,  0.U,  0.U,
                          5.U,    5.U,    5.U,    5.U,    5.U,    5.U,  5.U,  5.U,
                          10.U,   10.U,   10.U,   10.U,   10.U,   10.U, 10.U, 10.U,
                          15.U,   15.U,   15.U,   15.U,   15.U,   15.U, 15.U,
                          50.U,   50.U,   50.U,   50.U,   50.U,   50.U, 50.U, 50.U,
                          70.U,   70.U,   70.U,   70.U,   70.U,   70.U, 70.U)

    simulate(new InOrder) { c =>
      c.io.inst_in.poke(0.U)
      for (i <- 0 until expected_a1.size) {
        // Fetch instruction
        val pc = c.io.pc.peek().litValue.toInt/4
        // Step the clock
        c.clock.step()
        if (pc < instruction_cache.size) {
          c.io.inst_in.poke(instruction_cache(pc).S(32.W).asUInt)
        } else {
          c.io.inst_in.poke(0.U)
        }

        println(f"${c.io.this_pc.peek().litValue}\t${c.io.rf(11).peek().litValue}\t${c.io.rf(12).peek().litValue}\t${c.io.debug.peek().litValue.toString(2)}")
        c.io.rf(11).expect(expected_a1(i))
      }
    }
  }

  "6: Loads and stores" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test06.bin")
    val expected_a1 = Seq(0.U,    0.U,    0.U,    0.U,    0.U,  0.U,  0.U,  0.U,
                          5.U,    5.U,    5.U,    5.U,    5.U,  5.U,  5.U,
                          70.U,   70.U,   70.U,   70.U,   70.U, 70.U, 70.U)

    simulate(new InOrder) { c =>
      for (i <- 0 until expected_a1.size) {
        // Fetch instruction
        val pc = c.io.pc.peek().litValue.toInt/4
        // Step the clock
        c.clock.step()
        if (pc < instruction_cache.size) {
          c.io.inst_in.poke(instruction_cache(pc).S(32.W).asUInt)
        } else {
          c.io.inst_in.poke(0.U)
        }

        // println(f"${c.io.pc.peek().litValue}\t${c.io.write_mask.peek().litValue}\t${c.io.addr.peek().litValue}\t${c.io.write.peek().litValue}")
        // c.io.rf(11).expect(expected_a1(i))
      }
    }
  }
}
