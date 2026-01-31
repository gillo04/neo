package out_of_order 

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

class RsEntry(rob_addr_bits: Int) extends Bundle {
  val valid =       Bool()

  val s1_valid =    Bool()
  val s1_name =     UInt(rob_addr_bits.W)

  val s2_valid =    Bool()
  val s2_name =     UInt(rob_addr_bits.W)

  val d_name =      UInt(rob_addr_bits.W)

  val inst =        new Control
}

class ReservationStation(rs_addr_bits: Int, rob_addr_bits: Int, pip_ports_count: Int) extends Module {
  val buffer_size = math.pow(2, rs_addr_bits).toInt

  val io = IO(new Bundle{
    // Input port
    val valid_inst =      Input(Bool())
    val rs_ready =        Output(Bool())
    val inst =            Input(new Control)

    // Issue port
    val valid_issue =     Output(Bool())
    val dest_name =       Output(UInt(rob_addr_bits.W))
    val issue =           Output(new RsEntry(rob_addr_bits))

    // Rf port
    val rf_src1 =         Output(UInt(5.W))
    val rf_name1 =        Input(UInt(rob_addr_bits.W))
    val rf_valid1 =       Input(Bool())
    val rf_src2 =         Output(UInt(5.W))
    val rf_name2 =        Input(UInt(rob_addr_bits.W))
    val rf_valid2 =       Input(Bool())
    val rf_dest =         Output(UInt(5.W))
    val rf_dest_valid =   Output(Bool())
    val rf_new_name =     Output(UInt(rob_addr_bits.W))

    // Rob port
    val rob_ready =       Output(Bool())
    val rob_valid =       Input(Bool())
    val rob_addr =        Input(UInt(rob_addr_bits.W))

    // Update ports
    val pip_ports =       Input(Vec(pip_ports_count, new RobPipPort(rob_addr_bits)))

    // Debug
    val buffer =          Output(Vec(buffer_size, new RsEntry(rob_addr_bits)))
  })

  val buffer = RegInit(VecInit(Seq.fill(buffer_size)((new RsEntry(rob_addr_bits)).Lit(
    _.valid -> false.B,
    _.s1_valid -> false.B,
    _.s1_name -> 0.U,
    _.s2_valid -> false.B,
    _.s2_name -> 0.U,
    _.d_name -> 0.U,
    _.inst -> (new Control).Lit(
      _.src1 -> 0.U,
      _.src2 -> 0.U,
      _.dest -> 0.U,
      _.imm -> 0.U,
      _.alu_op -> 0.U,
      _.imm_mux -> false.B,
      _.mem_mux -> false.B,
      _.mem_size -> 0.U,
      _.mem_sx -> false.B,
      _.mem_store -> false.B,
      _.alu_d -> false.B,
      _.dest_valid_0 -> false.B,
      _.dest_valid_1 -> false.B,
    )
  ))))
  io.buffer := buffer

  // Allocation logic
  // Priority decoder over the free entries
  val free_entry_valid = buffer.foldLeft(false.B)((x, y) => x | !y.valid)
  val free_entry = PriorityEncoder(VecInit(buffer.map(i => !i.valid)).asUInt)

  // Issue instruction
  // Priority decoder over the ready instructions
  val issue_entry_valid = buffer.foldLeft(false.B)((x, y) => x | (y.valid & y.s1_valid & y.s2_valid))
  val issue_entry = PriorityEncoder(VecInit(buffer.map(i => i.valid & i.s1_valid & i.s2_valid)).asUInt)

  // Inster instruction
  io.rs_ready := free_entry_valid
  when (io.valid_inst && free_entry_valid) {
    buffer(free_entry).valid := true.B
    buffer(free_entry).s1_valid := (
        io.rf_valid1 |                                                                      // Either its valid in the renamer
        io.pip_ports.foldLeft(false.B)((x, y) => x | (y.valid & y.addr === io.rf_name1))    // Or it will be valid on the next cycle
      ) & (buffer(issue_entry).inst.dest =/= io.inst.src1 | !issue_entry_valid) |           // And always it mustn't be invalidated by the outgoing instruction
      io.inst.src1 === 0.U
    buffer(free_entry).s2_valid := (
        io.rf_valid2 |                                                                      // Either its valid in the renamer
        io.pip_ports.foldLeft(false.B)((x, y) => x | (y.valid & y.addr === io.rf_name2))    // Or it will be valid on the next cycle
      ) & (buffer(issue_entry).inst.dest =/= io.inst.src2 | !issue_entry_valid) |           // And always it mustn't be invalidated by the outgoing instruction
      io.inst.src2 === 0.U
    buffer(free_entry).s1_name := io.rf_name1
    buffer(free_entry).s2_name := io.rf_name2
    buffer(free_entry).d_name := io.rob_addr
    buffer(free_entry).inst := io.inst
  }

  // Update validity
  for (i <- 0 until buffer_size) {
    when (io.pip_ports.foldLeft(false.B)((x, y) => x | (y.addr === buffer(i).s1_name & y.valid))) {
      buffer(i).s1_valid := true.B
    }

    when (io.pip_ports.foldLeft(false.B)((x, y) => x | (y.addr === buffer(i).s2_name & y.valid))) {
      buffer(i).s2_valid := true.B
    }
  }

  // Issue instruction
  io.valid_issue := issue_entry_valid
  io.issue := Mux(issue_entry_valid,
    buffer(issue_entry),
    (new RsEntry(rob_addr_bits)).Lit(
      _.valid -> false.B,
      _.s1_valid -> false.B,
      _.s1_name -> 0.U,
      _.s2_valid -> false.B,
      _.s2_name -> 0.U,
      _.d_name -> 0.U,
      _.inst -> (new Control).Lit(
        _.src1 -> 0.U,
        _.src2 -> 0.U,
        _.dest -> 0.U,
        _.imm -> 0.U,
        _.alu_op -> 0.U,
        _.imm_mux -> false.B,
        _.mem_mux -> false.B,
        _.mem_size -> 0.U,
        _.mem_sx -> false.B,
        _.mem_store -> false.B,
        _.alu_d -> false.B,
        _.dest_valid_0 -> false.B,
        _.dest_valid_1 -> false.B,
      )
    )
  )
  io.dest_name := buffer(issue_entry).d_name
  when (issue_entry_valid) {
    buffer(issue_entry).valid := false.B
  }

  // Rf ports
  io.rf_src1 := io.inst.src1
  io.rf_src2 := io.inst.src2

  io.rf_dest := io.inst.dest
  io.rf_dest_valid := io.inst.dest_valid_0 | io.inst.dest_valid_1
  io.rf_new_name := io.rob_addr

  // Rob ports
  io.rob_ready := io.valid_inst
}
