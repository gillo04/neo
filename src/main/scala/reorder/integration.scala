package reorder

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

class Integration extends Module {
  val io = IO(new Bundle{
    // Debug
    val rf =          Output(Vec(32, new RfEntry(6)))
    val buffer =      Output(Vec(64, new RobEntry))
    val pc =          Output(UInt(32.W))
    val addr  =       Input(UInt(10.W))
    val wdata =       Input(UInt(32.W))
    val wen   =       Input(Bool())
    val stall   =     Output(Bool())

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
  })
  val pipeline = Module(new Reorder)
  io.rf := pipeline.io.rf
  io.pc := pipeline.io.pc
  io.buffer := pipeline.io.buffer
  io.stall := pipeline.io.stall

  // Instruction cache
  val i_cache = Module(new Cache)
  pipeline.io.inst_in := i_cache.io.rdata
  // Connect it to the debug port
  when (io.wen) {
    i_cache.io.addr := io.addr
  } .otherwise {
    i_cache.io.addr := pipeline.io.pc(31, 2)
  }

  i_cache.io.wdata := io.wdata
  i_cache.io.wen := io.wen
  i_cache.io.be := "b1111".U

  // Data cache
  val d_cache = Module(new Cache)
  pipeline.io.read := d_cache.io.rdata
  d_cache.io.addr := pipeline.io.addr
  d_cache.io.wdata := pipeline.io.write
  d_cache.io.be := pipeline.io.write_mask
  d_cache.io.wen := pipeline.io.write_en

  // Debug
  io.s1_p0 := pipeline.io.s1_p0
  io.s2_p0 := pipeline.io.s2_p0
  io.rd_p0 := pipeline.io.rd_p0
  io.imm_p0 := pipeline.io.imm_p0
  io.alu_d_p0 := pipeline.io.alu_d_p0
  io.dest_valid_p0 := pipeline.io.dest_valid_p0

  io.s1_p1 := pipeline.io.s1_p1
  io.s2_p1 := pipeline.io.s2_p1
  io.rd_p1 := pipeline.io.rd_p1
  io.imm_p1 := pipeline.io.imm_p1
  io.alu_d_p1 := pipeline.io.alu_d_p1
  io.dest_valid_p1 := pipeline.io.dest_valid_p1

  io.s1_p2 := pipeline.io.s1_p2
  io.s2_p2 := pipeline.io.s2_p2
  io.rd_p2 := pipeline.io.rd_p2
  io.imm_p2 := pipeline.io.imm_p2
  io.alu_d_p2 := pipeline.io.alu_d_p2
  io.dest_valid_p2 := pipeline.io.dest_valid_p2
}

object Integration extends App {
  ChiselStage.emitSystemVerilogFile(
    new Integration,
    Array("--target-dir", "builds"),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info", "-default-layer-specialization=enable")
  )
}
