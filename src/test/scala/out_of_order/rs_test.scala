package out_of_order 

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import java.nio.file.{Files, Paths}
import java.nio.{ByteBuffer, ByteOrder}

class RsTest extends AnyFreeSpec with Matchers with ChiselSim {
  "Should properly insert and remove instructions" in {
    simulate(new ReservationStation(4, 6, 2)) { c =>
      c.io.pip_ports(0).valid.poke(false.B)
      c.io.pip_ports(1).valid.poke(false.B)

      // Insert two adds with the same operands, invalid
      // add1 x3, x1, x2
      c.io.valid_inst.poke(true.B)

      c.io.inst.src1.poke(1.U)
      c.io.inst.src2.poke(2.U)
      c.io.inst.dest.poke(3.U)
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

      c.io.rf_src1.expect(1.U)
      c.io.rf_name1.poke(11.U)
      c.io.rf_valid1.poke(false.B)

      c.io.rf_src2.expect(2.U)
      c.io.rf_name2.poke(12.U)
      c.io.rf_valid2.poke(false.B)

      c.io.rob_ready.expect(true.B)
      c.io.rob_valid.poke(true.B)
      c.io.rob_addr.poke(13.U)
      c.io.rf_new_name.expect(13.U)

      c.io.valid_issue.expect(false.B)

      c.clock.step()

      // add1 x3, x1, x2
      c.io.valid_inst.poke(true.B)

      c.io.inst.src1.poke(1.U)
      c.io.inst.src2.poke(2.U)
      c.io.inst.dest.poke(3.U)
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

      c.io.rf_src1.expect(1.U)
      c.io.rf_name1.poke(11.U)
      c.io.rf_valid1.poke(false.B)

      c.io.rf_src2.expect(2.U)
      c.io.rf_name2.poke(12.U)
      c.io.rf_valid2.poke(false.B)

      c.io.rob_ready.expect(true.B)
      c.io.rob_valid.poke(true.B)
      c.io.rob_addr.poke(23.U)
      c.io.rf_new_name.expect(23.U)

      c.io.valid_issue.expect(false.B)

      c.clock.step()

      // Validate both operands
      c.io.valid_inst.poke(false.B)
      c.io.pip_ports(1).valid.poke(true.B)
      c.io.pip_ports(1).addr.poke(11.U)
      c.io.pip_ports(1).dest.poke(1.U)
      c.io.valid_issue.expect(false.B)
      c.clock.step()

      c.io.pip_ports(1).valid.poke(true.B)
      c.io.pip_ports(1).addr.poke(12.U)
      c.io.pip_ports(1).dest.poke(2.U)
      c.io.valid_issue.expect(false.B)
      c.clock.step()

      // Check the istructions are issued in order, with the correct sinks
      c.io.pip_ports(1).valid.poke(false.B)
      c.io.valid_issue.expect(true.B)
      c.io.issue.inst.src1.expect(1.U)
      c.io.issue.inst.src2.expect(2.U)
      c.io.issue.inst.dest.expect(3.U)
      c.io.dest_name.expect(13.U)
      c.clock.step()

      c.io.valid_issue.expect(true.B)
      c.io.issue.inst.src1.expect(1.U)
      c.io.issue.inst.src2.expect(2.U)
      c.io.issue.inst.dest.expect(3.U)
      c.io.dest_name.expect(23.U)
      c.clock.step()

      // Insert two adds with different operands
      // add1 x3, x4, x5
      c.io.valid_inst.poke(true.B)

      c.io.inst.src1.poke(4.U)
      c.io.inst.src2.poke(5.U)
      c.io.inst.dest.poke(3.U)
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

      c.io.rf_src1.expect(4.U)
      c.io.rf_name1.poke(14.U)
      c.io.rf_valid1.poke(false.B)

      c.io.rf_src2.expect(5.U)
      c.io.rf_name2.poke(15.U)
      c.io.rf_valid2.poke(false.B)

      c.io.rob_ready.expect(true.B)
      c.io.rob_valid.poke(true.B)
      c.io.rob_addr.poke(13.U)
      c.io.rf_new_name.expect(13.U)

      c.io.valid_issue.expect(false.B)

      c.clock.step()

      // add1 x3, x6, x7
      c.io.valid_inst.poke(true.B)

      c.io.inst.src1.poke(6.U)
      c.io.inst.src2.poke(7.U)
      c.io.inst.dest.poke(3.U)
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

      c.io.rf_src1.expect(6.U)
      c.io.rf_name1.poke(16.U)
      c.io.rf_valid1.poke(false.B)

      c.io.rf_src2.expect(7.U)
      c.io.rf_name2.poke(17.U)
      c.io.rf_valid2.poke(false.B)

      c.io.rob_ready.expect(true.B)
      c.io.rob_valid.poke(true.B)
      c.io.rob_addr.poke(33.U)
      c.io.rf_new_name.expect(33.U)

      c.io.valid_issue.expect(false.B)

      c.clock.step()

      // Validate the operands of the second
      c.io.valid_inst.poke(false.B)
      c.io.pip_ports(1).valid.poke(true.B)
      c.io.pip_ports(1).addr.poke(16.U)
      c.io.pip_ports(1).dest.poke(6.U)
      c.io.valid_issue.expect(false.B)
      c.clock.step()

      c.io.pip_ports(1).valid.poke(true.B)
      c.io.pip_ports(1).addr.poke(17.U)
      c.io.pip_ports(1).dest.poke(7.U)
      c.io.valid_issue.expect(false.B)
      c.clock.step()

      // Check it is issued before the first
      c.io.pip_ports(1).valid.poke(false.B)
      c.io.valid_issue.expect(true.B)
      c.io.issue.inst.src1.expect(6.U)
      c.io.issue.inst.src2.expect(7.U)
      c.io.issue.inst.dest.expect(3.U)
      c.io.dest_name.expect(33.U)
      c.clock.step()
      
      // Insert an instruction with one valid operand
      // add x3, x8, x9
      c.io.valid_inst.poke(true.B)

      c.io.inst.src1.poke(8.U)
      c.io.inst.src2.poke(9.U)
      c.io.inst.dest.poke(3.U)
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

      c.io.rf_src1.expect(8.U)
      c.io.rf_name1.poke(18.U)
      c.io.rf_valid1.poke(false.B)

      c.io.rf_src2.expect(9.U)
      c.io.rf_name2.poke(19.U)
      c.io.rf_valid2.poke(true.B)

      c.io.rob_ready.expect(true.B)
      c.io.rob_valid.poke(true.B)
      c.io.rob_addr.poke(43.U)
      c.io.rf_new_name.expect(43.U)

      c.io.valid_issue.expect(false.B)

      c.clock.step()
      
      // Validate the second operand
      c.io.valid_inst.poke(false.B)
      c.io.pip_ports(1).valid.poke(true.B)
      c.io.pip_ports(1).addr.poke(18.U)
      c.io.pip_ports(1).dest.poke(8.U)
      c.io.valid_issue.expect(false.B)
      c.clock.step()

      // Check it is issued
      c.io.pip_ports(1).valid.poke(false.B)
      c.io.valid_issue.expect(true.B)
      c.io.issue.inst.src1.expect(8.U)
      c.io.issue.inst.src2.expect(9.U)
      c.io.issue.inst.dest.expect(3.U)
      c.io.dest_name.expect(43.U)
      c.clock.step()
    }
  }
}
