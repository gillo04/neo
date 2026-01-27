package out_of_order 

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import java.nio.file.{Files, Paths}
import java.nio.{ByteBuffer, ByteOrder}

class SchedulerTest extends AnyFreeSpec with Matchers with ChiselSim {
  "Scheduler should read and issue instructions correctly" in {
    simulate(new Scheduler(6, 2, 2)) { c =>
      // Fetch add x1, x0, x0
      c.io.valid_inst.poke(true.B)
      c.io.inst.src1.poke(0.U)
      c.io.inst.src2.poke(0.U)
      c.io.inst.dest.poke(1.U)
      c.io.inst.imm.poke(0.U)
      c.io.inst.alu_op.poke(0.U)
      c.io.inst.imm_mux.poke(false.B)
      c.io.inst.mem_mux.poke(false.B)
      c.io.inst.mem_size.poke(0.U)
      c.io.inst.mem_sx.poke(false.B)
      c.io.inst.mem_store.poke(false.B)
      c.io.inst.alu_d.poke(false.B)
      c.io.inst.dest_valid_0.poke(true.B)
      c.io.inst.dest_valid_1.poke(false.B)

      c.clock.step()

      // Fetch add x2, x1, x0
      c.io.valid_inst.poke(true.B)
      c.io.inst.src1.poke(1.U)
      c.io.inst.src2.poke(0.U)
      c.io.inst.dest.poke(2.U)
      c.io.inst.imm.poke(0.U)
      c.io.inst.alu_op.poke(0.U)
      c.io.inst.imm_mux.poke(false.B)
      c.io.inst.mem_mux.poke(false.B)
      c.io.inst.mem_size.poke(0.U)
      c.io.inst.mem_sx.poke(false.B)
      c.io.inst.mem_store.poke(false.B)
      c.io.inst.alu_d.poke(false.B)
      c.io.inst.dest_valid_0.poke(true.B)
      c.io.inst.dest_valid_1.poke(false.B)

      c.clock.step()
      
      // Check add x1, x0, x0 is issued
      c.io.valid_inst.poke(false.B)
      println(f"${c.io.issue.dest_valid_0.peek()}\t${c.io.issue.src1.peek()}\t${c.io.issue.src2.peek()}\t${c.io.issue.dest.peek()}")
      c.io.issue.dest_valid_0.expect(true.B)
      c.io.issue.src1.expect(0.U)
      c.io.issue.src2.expect(0.U)
      c.io.issue.dest.expect(1.U)

      c.clock.step()

      // Check add x2, x1, x0 is not issued
      c.io.issue.dest_valid_0.expect(false.B)

      c.clock.step()
      c.io.valid_inst.poke(false.B)

      // Validate x1
      c.io.pip_ports(1).valid.poke(true.B)
      c.io.pip_ports(1).addr.poke(0.U)
      c.io.pip_ports(1).dest.poke(1.U)
      c.io.issue.dest_valid_0.expect(false.B)

      c.clock.step()
      c.io.pip_ports(1).valid.poke(false.B)
      c.clock.step()

      // Check add x2, x1, x0 is issued
      c.io.issue.dest_valid_0.expect(true.B)
      println(f"${c.io.issue.dest_valid_0.peek()}\t${c.io.issue.src1.peek()}\t${c.io.issue.src2.peek()}\t${c.io.issue.dest.peek()}")
      c.io.issue.src1.expect(1.U)
      c.io.issue.src2.expect(0.U)
      c.io.issue.dest.expect(2.U)

      c.clock.step()

      // Check no more instructions are issued
    }
  }
}
