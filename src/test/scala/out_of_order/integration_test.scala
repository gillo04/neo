package out_of_order 

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

  def inspectPipeline(c: Integration) = {
    println(f"Pipeline inspection:")
    println(f"\tFetch: pc: ${c.io.pc.peek().litValue}\tstall: ${c.io.stall.peek().litValue}")
    println(f"\t\tSrc1\tSrc2\tDest\tImm\tALU dep\tDest valid")
    println(f"\tData:\t${c.io.s1_p0.peek().litValue}\t${c.io.s2_p0.peek().litValue}\t${c.io.rd_p0.peek().litValue}\t${c.io.imm_p0.peek().litValue}")
    println(f"\tALU:\t${c.io.s1_p1.peek().litValue}\t${c.io.s2_p1.peek().litValue}\t${c.io.rd_p1.peek().litValue}\t${c.io.imm_p1.peek().litValue}")
    println(f"\tMAU:\t\t\t${c.io.rd_p2.peek().litValue}\t")
  }

  def inspectData(c: Integration, regs: Seq[Int] = Seq(), addrs: Seq[Int] = Seq()) = {
    println("Register File:")
    println("\tReg\tValid\tValue\tName")
    if (regs.size == 0) {
      for (i <- 0 until 32) {
        println(f"\tx$i:\t${c.io.rf(i).valid.peek().litValue}\t${c.io.rf(i).value.peek().litValue}\t${c.io.rf(i).name.peek().litValue}")
      }
    } else {
      for (i <- regs) {
        println(f"\tx$i:\t${c.io.rf(i).valid.peek().litValue}\t${c.io.rf(i).value.peek().litValue}\t${c.io.rf(i).name.peek().litValue}")
      }
    }

    println("Reorder Buffer:")
    println("\tAddr\tBusy\tValid\tValue\tDest")
    if (regs.size == 0) {
      for (i <- 0 until 64) {
        println(f"\t$i:\t${c.io.buffer(i).busy.peek().litValue}\t${c.io.buffer(i).valid.peek().litValue}\t${c.io.buffer(i).value.peek().litValue}\t${c.io.buffer(i).dest.peek().litValue}")
      }
    } else {
      for (i <- addrs) {
        println(f"\t$i:\t${c.io.buffer(i).busy.peek().litValue}\t${c.io.buffer(i).valid.peek().litValue}\t${c.io.buffer(i).value.peek().litValue}\t${c.io.buffer(i).dest.peek().litValue}")
      }
    }
  }

  def inspectRs(c: Integration, entries: Seq[Int] = Seq()) = {
    println("Reservation Station:")
    println("\tEntry\tValid\tS1\tV S1\tS2\tV S2\tDest")
    if (entries.size == 0) {
      for (i <- 0 until 8) {
        println(f"\t$i:\t${c.io.rs(i).valid.peek().litValue}\t${c.io.rs(i).inst.src1.peek().litValue}\t${c.io.rs(i).s1_valid.peek().litValue}\t${c.io.rs(i).inst.src2.peek().litValue}\t${c.io.rs(i).s2_valid.peek().litValue}\t${c.io.rs(i).inst.dest.peek().litValue}")
      }
    } else {
      for (i <- entries) {
        println(f"\t$i:\t${c.io.rs(i).valid.peek().litValue}\t${c.io.rs(i).inst.src1.peek().litValue}\t${c.io.rs(i).s1_valid.peek().litValue}\t${c.io.rs(i).inst.src2.peek().litValue}\t${c.io.rs(i).s2_valid.peek().litValue}\t${c.io.rs(i).inst.dest.peek().litValue}")
      }
    }
  }

  "0: Addition with dependencies" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test00.bin")

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
      while (c.io.rf(11).value.peek().litValue != 142) {
        // Step the clock
        // inspectData(c, Seq(1, 10, 11), Seq(0, 1, 2))
        // inspectPipeline(c)
        // println("====================================================")
        c.clock.step()
      }
      c.io.rf(11).value.expect(142.U)
    }
  }

  "1: Unconditional branching" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test01.bin")

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
      while (c.io.rf(15).value.peek().litValue == 0) {
        // Step the clock
        // inspectData(c, Seq(0, 10, 11, 12, 13, 14), Seq(0, 1, 2, 3, 4, 5))
        // inspectPipeline(c)
        // println("====================================================")
        c.clock.step()
      }
      c.io.rf(15).value.expect(1111.U)
    }
  }

  "2: JAL saves return address" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test02.bin")

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
      while (c.io.rf(10).value.peek().litValue == 0) {
        // Step the clock
        // inspectData(c, Seq(0, 10, 11, 12, 13, 14), Seq(0, 1, 2, 3, 4, 5))
        // inspectPipeline(c)
        // println("====================================================")
        c.clock.step()
      }
      c.io.rf(10).value.expect((4096 + 12).U)
    }
  }

  "3: JALR" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test03.bin")

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
      while (c.io.rf(12).value.peek().litValue == 0) {
        // Step the clock
        // inspectData(c, Seq(0, 10, 11, 12, 13, 14), Seq(0, 1, 2, 3, 4, 5))
        // inspectRs(c, Seq(0, 1, 2, 3))
        // inspectPipeline(c)
        // println("====================================================")
        c.clock.step()
      }
      c.io.rf(12).value.expect(20.U)
    }
  }

  "4: LUI and AUIPC" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test04.bin")

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
      while (c.io.rf(11).value.peek().litValue == 0) {
        // Step the clock
        // inspectData(c, Seq(0, 10, 11, 12, 13, 14), Seq(0, 1, 2, 3, 4, 5))
        // inspectPipeline(c)
        // println("====================================================")
        c.clock.step()
      }
      c.io.rf(10).value.expect(4096.U)
      c.io.rf(11).value.expect((4096 + 4100).U)
    }
  }

  "5: Conditional branching" in {
    // Load instructions from file
    val instruction_cache = instructionsFromFile("./test_files/test05.bin")

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
      val expected = Seq(0, 5, 10, 15, /*50, removed because it doesn't get commited to the RF, but its caluclated*/ 70)
      var i = 0
      while (i < expected.size - 1) {
        if (c.io.rf(11).value.peek().litValue != expected(i)) {
          i += 1
        }
        c.io.rf(11).value.expect(expected(i).U)
        // Step the clock
        // inspectData(c, Seq(0, 10, 11, 12, 13, 14), Seq(10, 11, 12, 13, 14, 15, 16, 17, 18, 19))
        // inspectPipeline(c)
        // println(f"$i====================================================")
        c.clock.step()
      }
      c.io.rf(11).value.expect(70.U)
    }
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
      var i = 0
      while (c.io.rf(14).value.peek().litValue == 0) {
        // Step the clock
        c.clock.step()
        // inspectData(c, Seq(0, 10, 11, 12, 13, 14), Seq(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
        // inspectRs(c, Seq(0, 1, 2, 3))
        // inspectPipeline(c)
        // println(f"$i====================================================")
        i += 1
      }
      c.io.rf(14).value.expect(89.U)
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
      while (c.io.rf(14).value.peek().litValue == 0) {
        // Step the clock
        c.clock.step()
        // inspectData(c, Seq(0, 10, 11, 12, 13, 14), Seq(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
        // inspectRs(c, Seq(0, 1, 2, 3))
        // inspectPipeline(c)
        // println("====================================================")
      }
      c.io.rf(14).value.expect(89.U)
    }
  }
}
