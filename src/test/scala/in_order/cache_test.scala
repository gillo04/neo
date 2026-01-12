package in_order

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import java.nio.file.{Files, Paths}
import java.nio.{ByteBuffer, ByteOrder}

/*
    val addr  = Input(UInt(9.W))
    val wdata = Input(UInt(32.W))
    val rdata = Output(UInt(32.W))
    val wen   = Input(Bool())
    val be    = Input(UInt(4.W)) // byte enables
    */

class CacheTest extends AnyFreeSpec with Matchers with ChiselSim {
  "Read and write work correctly" in {
    simulate(new Cache) { c =>
      c.io.addr.poke(5.U)
      c.io.wdata.poke(0x0.U)
      c.io.wen.poke(true.B)
      c.io.be.poke("b1111".U)
      // println(f"${c.io.rdata.peek()}")
      c.clock.step()
      c.io.rdata.expect(0.U)
      
      c.io.addr.poke(5.U)
      c.io.wdata.poke(0xffff.U)
      c.io.wen.poke(true.B)
      c.io.be.poke("b0010".U)
      // println(f"${c.io.rdata.peek()}")
      c.clock.step()
      c.io.rdata.expect(0.U)

      c.io.addr.poke(5.U)
      c.io.wen.poke(false.B)
      // println(f"${c.io.rdata.peek()}")
      c.clock.step()
      c.io.rdata.expect(0xff00.U)

      // println(f"${c.io.rdata.peek()}")
      c.clock.step()
      c.io.rdata.expect(0xff00.U)
    }
  }
}
