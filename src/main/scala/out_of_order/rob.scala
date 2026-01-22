package out_of_order 

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

class RobEntry extends Bundle {
  val busy =      Bool()
  val valid =     Bool()
  val dest =      UInt(5.W)
  val value =     UInt(32.W)
}

class RobPipPort(addr_bits: Int) extends Bundle {
  val valid =     Bool()
  val addr =      UInt(addr_bits.W)
  val dest =      UInt(5.W)
  val value =     UInt(32.W)
}

class RobReq(addr_bits: Int) extends Bundle {
  val valid =     Bool()
  val addr =      UInt(addr_bits.W)
  val dest =      UInt(5.W)
  val value =     UInt(32.W)
}

// ReOrder Buffer
class Rob(addr_bits: Int, pip_ports_count: Int, inputs: Int) extends Module {
  val buffer_size = math.pow(2, addr_bits).toInt

  val io = IO(new Bundle{
    // Request ports
    val rq_ready =  Input(Bool())
    val rq_valid =  Output(Bool())
    val rq_addr =   Output(UInt(addr_bits.W))

    // Register file port
    val rf_valid =  Output(Bool())
    val rf_dest =   Output(UInt(5.W))
    val rf_value =  Output(UInt(32.W))
    val rf_name =   Output(UInt(addr_bits.W))

    // Pipeline ports
    val pip_ports = Input(Vec(pip_ports_count, new RobPipPort(addr_bits)))

    // Access
    val srcs =      Input(Vec(inputs, UInt(addr_bits.W)))
    val dests =     Output(Vec(inputs, new RobEntry))

    // Debug
    val buffer =    Output(Vec(buffer_size, new RobEntry))
  })

  // Buffer
  val buffer = RegInit(VecInit(Seq.fill(buffer_size)((new RobEntry).Lit(_.valid -> false.B, _.dest -> 0.U, _.value -> 0.U, _.busy -> false.B))))
  io.buffer := buffer

  // Pointers
  val top = RegInit(0.U(addr_bits.W))
  top := Mux(buffer(top).valid, top + 1.U, top)

  val bottom = RegInit(0.U(addr_bits.W))
  val rq_valid = !buffer(bottom).busy
  bottom := Mux(rq_valid & io.rq_ready, bottom + 1.U, bottom)
  io.rq_addr := bottom
  io.rq_valid := rq_valid
  when (!buffer(bottom).busy & io.rq_ready) {
    buffer(bottom).valid := false.B
    buffer(bottom).busy := true.B
  }

  // Register file
  io.rf_valid := buffer(top).valid
  io.rf_dest := buffer(top).dest
  io.rf_value := buffer(top).value
  io.rf_name := top
  when (buffer(top).busy && buffer(top).valid) {
    buffer(top).busy := false.B
  }

  // Pipeline
  for (i <- 0 until pip_ports_count) {
    when (io.pip_ports(i).valid) {
      buffer(io.pip_ports(i).addr).valid := true.B
      buffer(io.pip_ports(i).addr).dest := io.pip_ports(i).dest
      buffer(io.pip_ports(i).addr).value := io.pip_ports(i).value
    }
  }

  // Access
  for (i <- 0 until inputs) {
    io.dests(i) := buffer(io.srcs(i))
  }
}
