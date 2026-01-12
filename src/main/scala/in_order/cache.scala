package in_order

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage
import chisel3.util.experimental.loadMemoryFromFileInline

// Cache that synthesizes to a single Xilinx BRAM36 
class Cache(memory_file: String = "") extends Module {
  val io = IO(new Bundle {
    val addr  = Input(UInt(10.W))
    val wdata = Input(UInt(32.W))
    val rdata = Output(UInt(32.W))
    val wen   = Input(Bool())
    val be    = Input(UInt(4.W)) // byte enables
  })

  val mem = SyncReadMem(1024, Vec(4, UInt(8.W)))

  // Write with byte enables
  when(io.wen) {
    mem.write(io.addr, io.wdata.asTypeOf(Vec(4, UInt(8.W))), io.be.asBools)
  }

  // Synchronous read
  io.rdata := mem.read(io.addr, !io.wen).asUInt

  if (memory_file.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, memory_file)
  }
}

/*object Cache extends App {
  ChiselStage.emitSystemVerilogFile(
    new Cache,
    Array("--target-dir", "builds"),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info", "-default-layer-specialization=enable")
  )
}*/
