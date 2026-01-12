package reorder

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

class Integration extends Module {
  val io = IO(new Bundle{
    // Debug
    val rf =    Output(Vec(32, UInt(32.W)))
    val pc =    Output(UInt(32.W))
    val addr  = Input(UInt(10.W))
    val wdata = Input(UInt(32.W))
    val wen   = Input(Bool())
  })
  val pipeline = Module(new Reorder)
  io.rf := pipeline.io.rf
  io.pc := pipeline.io.pc

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
}

object Integration extends App {
  ChiselStage.emitSystemVerilogFile(
    new Integration,
    Array("--target-dir", "builds"),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info", "-default-layer-specialization=enable")
  )
}
