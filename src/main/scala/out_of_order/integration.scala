package out_of_order 

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

class Integration(debug: Boolean) extends Module {
  val io = IO(new Bundle{
    val x31 =           Output(UInt(16.W))
    val write_bit =     Input(UInt(1.W)) // Needed for the circuit not to be optimized away

    // Debug
    val debug_sig = if (debug) Some(new Bundle {
      val rf =          Output(Vec(32, new RfEntry(6)))
      val buffer =      Output(Vec(64, new RobEntry))
      val rs =          Output(Vec(8, new RsEntry(6)))
      val pc =          Output(UInt(32.W))
      val stall   =     Output(Bool())

      val addr  =       Input(UInt(10.W))
      val wdata =       Input(UInt(32.W))
      val wen   =       Input(Bool())

      val s1_p0 =       Output(UInt(5.W))
      val s2_p0 =       Output(UInt(5.W))
      val rd_p0 =       Output(UInt(5.W))
      val imm_p0 =      Output(UInt(32.W))
      val alu_d_p0 =    Output(Bool())
      val dest_valid_p0 = Output(Bool())

      val s1_p1 =       Output(UInt(32.W))
      val s2_p1 =       Output(UInt(32.W))
      val rd_p1 =       Output(UInt(5.W))
      val imm_p1 =      Output(UInt(32.W))
      val alu_d_p1 =    Output(Bool())
      val dest_valid_p1 = Output(Bool())

      val s1_p2 =       Output(UInt(32.W))
      val s2_p2 =       Output(UInt(32.W))
      val rd_p2 =       Output(UInt(5.W))
      val imm_p2 =      Output(UInt(32.W))
      val alu_d_p2 =    Output(Bool())
      val dest_valid_p2 = Output(Bool())
    }) else None
  })
  val pipeline = Module(new OutOfOrder(true))
  io.x31 := pipeline.io.debug_sig.get.rf(31).value

  // Instruction cache
  val i_cache = Module(new Cache)
  pipeline.io.inst_in := i_cache.io.rdata
  
  // Connect it to the debug port
  if (debug) {
    when (io.debug_sig.get.wen) {
      i_cache.io.addr := io.debug_sig.get.addr
    } .otherwise {
      i_cache.io.addr := pipeline.io.pc(31, 2)
    }
    i_cache.io.wdata := io.debug_sig.get.wdata
    i_cache.io.wen := io.debug_sig.get.wen
  } else {
    i_cache.io.addr := pipeline.io.pc(31, 2)
    i_cache.io.wdata := Cat(0.U(31.W), io.write_bit)
    i_cache.io.wen := 0.B
  }
  i_cache.io.be := "b1111".U

  // Data cache
  val d_cache = Module(new Cache)
  pipeline.io.read := d_cache.io.rdata
  d_cache.io.addr := pipeline.io.addr
  d_cache.io.wdata := pipeline.io.write
  d_cache.io.be := pipeline.io.write_mask
  d_cache.io.wen := pipeline.io.write_en

  // Debug
  if (debug) {
    io.debug_sig.get.rf := pipeline.io.debug_sig.get.rf
    io.debug_sig.get.pc := pipeline.io.debug_sig.get.this_pc
    io.debug_sig.get.buffer := pipeline.io.debug_sig.get.buffer
    io.debug_sig.get.rs := pipeline.io.debug_sig.get.rs
    io.debug_sig.get.stall := pipeline.io.debug_sig.get.stall

    io.debug_sig.get.s1_p0 := pipeline.io.debug_sig.get.s1_p0
    io.debug_sig.get.s2_p0 := pipeline.io.debug_sig.get.s2_p0
    io.debug_sig.get.rd_p0 := pipeline.io.debug_sig.get.rd_p0
    io.debug_sig.get.imm_p0 := pipeline.io.debug_sig.get.imm_p0
    io.debug_sig.get.alu_d_p0 := pipeline.io.debug_sig.get.alu_d_p0
    io.debug_sig.get.dest_valid_p0 := pipeline.io.debug_sig.get.dest_valid_p0

    io.debug_sig.get.s1_p1 := pipeline.io.debug_sig.get.s1_p1
    io.debug_sig.get.s2_p1 := pipeline.io.debug_sig.get.s2_p1
    io.debug_sig.get.rd_p1 := pipeline.io.debug_sig.get.rd_p1
    io.debug_sig.get.imm_p1 := pipeline.io.debug_sig.get.imm_p1
    io.debug_sig.get.alu_d_p1 := pipeline.io.debug_sig.get.alu_d_p1
    io.debug_sig.get.dest_valid_p1 := pipeline.io.debug_sig.get.dest_valid_p1

    io.debug_sig.get.s1_p2 := pipeline.io.debug_sig.get.s1_p2
    io.debug_sig.get.s2_p2 := pipeline.io.debug_sig.get.s2_p2
    io.debug_sig.get.rd_p2 := pipeline.io.debug_sig.get.rd_p2
    io.debug_sig.get.imm_p2 := pipeline.io.debug_sig.get.imm_p2
    io.debug_sig.get.alu_d_p2 := pipeline.io.debug_sig.get.alu_d_p2
    io.debug_sig.get.dest_valid_p2 := pipeline.io.debug_sig.get.dest_valid_p2
  }
}

object Integration extends App {
  ChiselStage.emitSystemVerilogFile(
    new Integration(false),
    Array("--target-dir", "builds"),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info", "-default-layer-specialization=enable")
  )
}
